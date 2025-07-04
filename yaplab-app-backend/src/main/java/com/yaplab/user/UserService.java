package com.yaplab.user;

import com.yaplab.chatroom.ChatRoom;
import com.yaplab.chatroom.ChatRoomRepository;
import com.yaplab.enums.UserStatus;
import com.yaplab.files.FilesRepository;
import com.yaplab.group.Group;
import com.yaplab.group.GroupRepository;
import com.yaplab.message.MessageRepository;
import com.yaplab.message.UserMessageStatusRepository;
import com.yaplab.security.authentication.*;
import com.yaplab.security.token.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for handling user-related operations such as registration, finding user by id or email, update details, etc.
 */
@Service
public class UserService {

    /**
     * Logger for UserService
     * This logger is used to log various events and errors in the UserService class.
     * It helps in debugging and tracking the flow of operations related to user management.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Constructor based dependency injection
     */
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final UserMessageStatusRepository userMessageStatusRepository;
    private final FilesRepository filesRepository;

    public UserService(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder,
                       EmailService emailService, EmailVerificationTokenRepository emailVerificationTokenRepository, 
                       RefreshTokenRepository refreshTokenRepository, SimpMessagingTemplate messagingTemplate, 
                       ChatRoomRepository chatRoomRepository, GroupRepository groupRepository, 
                       MessageRepository messageRepository, UserMessageStatusRepository userMessageStatusRepository,
                       FilesRepository filesRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.messagingTemplate = messagingTemplate;
        this.chatRoomRepository = chatRoomRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.userMessageStatusRepository = userMessageStatusRepository;
        this.filesRepository = filesRepository;
    }

    /**
     * Registers a new user and saves to the database.
     * Password is encoded and saved in the database using password encoder.
     * A random token is generated which expires in 30 minutes and sent to the user's email ID for verification.
     * The user also receives a welcome email.
     * @param registerRequestDTO A DTO with fields given by user to register
     * @return A responseDTO sent in response
     */
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        Optional<User> existingUser = userRepository.findByEmailId(registerRequestDTO.emailId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isEmailVerified()) {
                throw new IllegalArgumentException("User already exists and is verified. Please login instead.");
            } else {
                List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository.findByUser(user);
                boolean hasValidToken = existingTokens.stream()
                        .anyMatch(token -> token.getExpiryDate().isAfter(Instant.now()));

                if (hasValidToken) {
                    return new RegisterResponseDTO(
                            user.getId(),
                            user.getUserName(),
                            user.getEmailId(),
                            user.getMobileNumber(),
                            user.getStatus(),
                            "Registration reminder: Please check your email for the verification link we sent earlier."
                    );
                } else {
                    resendVerificationEmail(registerRequestDTO.emailId());
                    return new RegisterResponseDTO(
                            user.getId(),
                            user.getUserName(),
                            user.getEmailId(),
                            user.getMobileNumber(),
                            user.getStatus(),
                            "Your previous verification link expired. A new verification email has been sent."
                    );
                }
            }
        }

        User user = userMapper.toEntityFromRegisterRequest(registerRequestDTO);
        user.setStatus(UserStatus.OFFLINE);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(1800);
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, expiry, user);
        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink = "http://localhost:8080/auth/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmailId(), verificationLink);
        emailService.sendWelcomeEmail(user.getEmailId(), user.getUserName());
        return userMapper.toRegisterResponseDTO(user);
    }

    /**
     * Broadcasts user status to other users
     * @param userId ID of the user
     * @param status online or offline status
     * @param lastSeen last seen time
     */
    private void broadcastUserStatus(Long userId, String status, Instant lastSeen) {
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("id", userId);
            statusUpdate.put("userStatus", status);
            statusUpdate.put("lastSeen", lastSeen != null ? lastSeen.toString() : null);
            messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);
        } catch (Exception e) {
            logger.error("Failed to broadcast user status having ID{}", userId);
        }
    }


    /**
     * Sets user status to online when connecting and broadcasts the status to other users.
     */
    public void connect(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.ONLINE);
            user.setLastSeen(null);
            userRepository.save(user);
            broadcastUserStatus(userId, "ONLINE", user.getLastSeen());
        });
    }

    /**
     * Sets the status of the user to offline and broadcasts the status to other users.
     */
    public void disconnect(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.OFFLINE);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
            broadcastUserStatus(userId, "OFFLINE", user.getLastSeen());
        });
    }

    /**
     * Get comprehensive status information using direct repository access to avoid circular dependency
     * This method retrieves the status of all users in the chat rooms the current user is part of.
     * Outer map key is user ID, inner map contains status, last seen, and username.
     * Creates a map of user IDs to their status information.
     * @return a map where the key is the user ID and the value is a map containing status, last seen, and username.
     */
    public Map<Long, Map<String, Object>> getComprehensiveUserStatuses(Long currentUserId) {
        Map<Long, Map<String, Object>> statusMap = new HashMap<>();

        try {
            User currentUser = getUserEntityByID(currentUserId);
            List<ChatRoom> userChatRooms = chatRoomRepository.findAllByParticipantsContaining(currentUser);

            Set<Long> chatUserIds = new HashSet<>();
            for (ChatRoom chatRoom : userChatRooms) {
                if (chatRoom.getParticipants() != null) {
                    for (User participant : chatRoom.getParticipants()) {
                        if (!participant.getId().equals(currentUserId)) {
                            chatUserIds.add(participant.getId());
                        }
                    }
                }
            }

            for (Long userId : chatUserIds) {
                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    Map<String, Object> statusInfo = new HashMap<>();
                    statusInfo.put("userStatus", user.getStatus().toString());
                    statusInfo.put("lastSeen", user.getLastSeen());
                    statusInfo.put("userName", user.getUserName());

                    statusMap.put(user.getId(), statusInfo);
                }
            }
            return statusMap;

        } catch (Exception e) {
            logger.error("Error getting comprehensive user statuses for user {}", currentUserId, e);
            return new HashMap<>();
        }
    }

    /**
     * Gets the user by the given ID from the database.
     * This method is used to fetch user details for various purposes.
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public UserResponseDTO getUserByID(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found for the given ID"));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Gets the user from the database using userId
     * @param id The userId of the user
     * @return The User object
     */
    public User getUserEntityByID(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found for the given ID"));
    }

    /**
     * Finds the list of users with the searched set of characters with priority ordering
     * User can search by emailId or username or by mobile number.
     * Results are ordered by relevance: name matches first, then email, then mobile.
     * @param input the inputted set of characters
     * @param currentUserId the ID of the user performing the search (to exclude from results)
     * @return the list of User response DTO objects ordered by relevance
     */
    public List<UserResponseDTO> findUser(String input, Long currentUserId) {
        List<User> users = userRepository.findUsersWithPriority(input);

        return users
                .stream()
                .filter(User::isEmailVerified)
                .filter(user -> !user.getId().equals(currentUserId))
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets the user by the given emailId for recovery purpose.
     * @param emailId EmailId of the user.
     * @return the User found.
     */
    public UserResponseDTO getUserByEmail(String emailId) {
        User user = userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for the given email ID"));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Returns user entity from the email provided
     * @param emailId email ID of the user
     */
    public User getUserEntityByEmail(String emailId) {
        return userRepository.findByEmailId(emailId)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", emailId);
                    return new IllegalArgumentException("User not found with the emailId: " + emailId);
                });
    }

    /**
     * Updates the required details of the user and saves in the database.
     * This method is used to update user details such as username, emailId, and mobile number.
     * Sets the updated time to the current time.
     * @param userDTO The updated user to save details to.
     * @return the old user with updated details.
     * @throws IllegalArgumentException if user is not found or email already in use.
     */
    public UserResponseDTO updateUser(UserDTO userDTO) {
        return updateUserWithNotifications(userDTO);
    }

    /**
     * Deletes the user from the database for privacy and storage management.
     * This method is used to remove a user from the system.
     * @param id ID of the user to delete.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Delete failed: User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found with ID: " + id);
                });
        
        String userEmail = user.getEmailId();
        String userName = user.getUserName();
        
        // Handle groups created by this user - delete them as they cannot exist without a creator
        List<Group> userCreatedGroups = groupRepository.findByCreatedBy(user);
        
        // First, delete chat rooms associated with the groups created by this user
        for (Group group : userCreatedGroups) {
            List<ChatRoom> groupChatRooms = chatRoomRepository.findByGroup(group);
            for (ChatRoom chatRoom : groupChatRooms) {

                // Delete user message statuses for this chatroom before deleting the chatroom
                userMessageStatusRepository.deleteByMessageChatroomId(chatRoom.getChatroomId());
                chatRoomRepository.delete(chatRoom);
            }
        }

        groupRepository.removeUserFromAllGroups(user.getId());
        groupRepository.deleteGroupsByCreatedBy(user.getId());

        List<ChatRoom> userChatRooms = chatRoomRepository.findAllByParticipantsContaining(user);
        for (ChatRoom chatRoom : userChatRooms) {
            userMessageStatusRepository.deleteByUserIdAndMessageChatroomId(user.getId(), chatRoom.getChatroomId());
            
            chatRoom.getParticipants().remove(user);
            chatRoomRepository.save(chatRoom);
        }
        
        // Delete all messages sent by the user and their associated statuses
        // This is necessary to avoid foreign key constraint violations
        // While this removes chat history, it's required for user deletion to work
        userMessageStatusRepository.deleteByMessageSenderId(user.getId());
        userMessageStatusRepository.deleteByMessageReceiverId(user.getId());
        userMessageStatusRepository.deleteByUserId(user.getId());
        messageRepository.deleteBySenderId(user.getId());
        messageRepository.deleteByReceiverId(user.getId());
        filesRepository.deleteByUploadedBy(user);
        
        // Delete user tokens (email verification, password reset, refresh tokens, etc.)
        emailVerificationTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.deleteByUser(user);
        user.setStatus(UserStatus.OFFLINE);
        user.setLastSeen(Instant.now());
        userRepository.save(user);
        
        // Broadcast user offline status
        Map<String, Object> userStatusUpdate = new HashMap<>();
        userStatusUpdate.put("userStatus", UserStatus.OFFLINE);
        userStatusUpdate.put("lastSeen", user.getLastSeen());
        userStatusUpdate.put("id", user.getId());
        messagingTemplate.convertAndSend("/topic/user-status", userStatusUpdate);
        userRepository.deleteById(id);
        
        // Send account deletion notification
        emailService.sendAccountDeletionNotification(userEmail, userName);
        
        logger.info("User deleted successfully with ID: {} and deletion notification sent to: {}", id, userEmail);
    }

    /**
     * Finds the list of connected or disconnected users.
     * @param status status of the user
     * @return the list of user response DTO object.
     */
    public List<UserResponseDTO> findConnectedOrDisconnectedUsers(UserStatus status) {
        return userRepository.findByStatus(status)
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates the profile picture url with the file stored in the "uploads" folder.
     * Checks that the file size does not exceed 5MB and are of type png/jpeg/jpg.
     * Maintains consistent naming of all pictures.
     * Tries to create a parent directory if it doesn't exist
     * Uploads the file to the directory.
     * @param userId The user id of the person who wants to update profile picture.
     * @param file The file uploaded by the user.
     */
    public void updateProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Profile picture update failed: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg") ||
                        contentType.equalsIgnoreCase("image/png"))) {
            logger.warn("Profile picture update failed: Invalid file type for user {}", userId);
            throw new IllegalArgumentException("Only JPEG, JPG, and PNG files are allowed.");
        }
        long maxSize = 5 * 1024 * 1024; // 5MB

        if (file.getSize() > maxSize) {
            logger.warn("Profile picture update failed: File too large for user {}", userId);
            throw new IllegalArgumentException("File size must not exceed 5MB.");
        }
        // Create absolute path for uploads/users directory
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path uploadsDir = projectRoot.resolve("uploads").resolve("users");
        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadsDir.resolve(fileName);
        
        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
            logger.info("Profile picture stored successfully at: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to store profile picture for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
        String profilePictureUrl = "/files/serve/users/" + fileName;
        user.setProfilePictureUrl(profilePictureUrl);
        userRepository.save(user);
        logger.info("Profile picture updated for user {}", userId);
    }

    /**
     * Gets the creation date of the user.
     * @param userId The ID of the user.
     * @return The Instant representing the creation date.
     */
    public Instant getUserCreationDate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Get creation date failed: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });
        return user.getCreatedAt();
    }

    /**
     * Gets the last update date of the user.
     * @param userId The ID of the user.
     * @return The Instant representing the last update date.
     */
    public Instant getUserLastUpdateDate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Get last update date failed: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });
        return user.getUpdatedAt();
    }

    /**
     * Checks if a user with the given email exists and returns information about their verification status
     * @param emailId The email to check
     * @return Map containing exists (boolean) and verified (boolean if exists)
     */
    public Map<String, Object> checkEmailStatus(String emailId) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOptional = userRepository.findByEmailId(emailId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            result.put("exists", true);
            result.put("verified", user.isEmailVerified());
        } else {
            result.put("exists", false);
        }

        return result;
    }

    /**
     * Resends the email verification link to the user.
     * Deletes existing tokens and creates a new one.
     * @param emailId The email address of the user
     */
    @Transactional
    public void resendVerificationEmail(String emailId) {
        User user = userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(1800); // 30 minutes

        EmailVerificationToken verificationToken = new EmailVerificationToken(token, expiry, user);
        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink = "http://localhost:8080/auth/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmailId(), verificationLink);
    }

    /**
     * Initiates the email change process by verifying the user's password and sending a confirmation link to the new email.
     * @param userId The ID of the user
     * @param newEmail The new email address
     * @param password The password of the user for verification
     */
    @Transactional
    public void initiateEmailChange(Long userId, String newEmail, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        emailVerificationTokenRepository.deleteByUserIdAndTokenType(userId, "EMAIL_CHANGE");

        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(1800);

        EmailVerificationToken emailChangeToken = new EmailVerificationToken(token, expiry, user, newEmail);
        emailVerificationTokenRepository.save(emailChangeToken);

        String confirmationLink = "http://localhost:5173/auth/confirm-email-change?token=" + token;
        emailService.sendEmailChangeVerification(newEmail, confirmationLink, user.getUserName());
    }

    /**
     * Confirms the email change using the verification token
     * @param token The email change verification token
     */
    @Transactional
    public void confirmEmailChange(String token) {
        EmailVerificationToken changeToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired email change token"));

        if (changeToken.isExpired()) {
            emailVerificationTokenRepository.delete(changeToken);
            throw new IllegalArgumentException("Email change token has expired");
        }

        if (changeToken.getTokenType() != EmailVerificationToken.TokenType.EMAIL_CHANGE) {
            throw new IllegalArgumentException("Invalid token type for email change");
        }

        User user = changeToken.getUser();
        String oldEmail = user.getEmailId();
        String newEmail = changeToken.getNewEmail();
        userRepository.findByEmailId(newEmail)
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(user.getId())) {
                        throw new IllegalArgumentException("Email address is already in use");
                    }
                });

        user.setEmailId(newEmail);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        emailVerificationTokenRepository.delete(changeToken);
    }

    /**
     * Updates the user details with notifications for email and mobile number changes.
     * This method checks if the email or mobile number has changed,
     */
    public UserResponseDTO updateUserWithNotifications(UserDTO userDTO) {
        User oldUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userDTO.id());
                    return new IllegalArgumentException("User not found with the id " + userDTO.id());
                });

        boolean mobileChanged = false;

        if (userDTO.emailId() != null && !userDTO.emailId().equals(oldUser.getEmailId())) {
            userRepository.findByEmailId(userDTO.emailId())
                    .ifPresent(user -> {
                        if (!user.getId().equals(oldUser.getId())) {
                            throw new IllegalArgumentException("Email - " + userDTO.emailId() + " already in use");
                        }
                    });
            oldUser.setEmailId(userDTO.emailId());
        }
        
        // Check and update mobile number if changed
        if (userDTO.mobileNumber() != null) {
            String trimmedMobileNumber = userDTO.mobileNumber().trim();
            
            if (trimmedMobileNumber.isEmpty()) {
                mobileChanged = oldUser.getMobileNumber() != null;
                oldUser.setMobileNumber(null);
            } else {
                if (!trimmedMobileNumber.matches("^[0-9]{10}$")) {
                    throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
                }
                
                // Check if this mobile number is already used by another user
                if (!trimmedMobileNumber.equals(oldUser.getMobileNumber())) {
                    userRepository.findByMobileNumber(trimmedMobileNumber)
                            .ifPresent(user -> {
                                if (!user.getId().equals(oldUser.getId())) {
                                    throw new IllegalArgumentException("Mobile number - " + trimmedMobileNumber + " already in use");
                                }
                            });
                    oldUser.setMobileNumber(trimmedMobileNumber);
                    mobileChanged = true;
                }
            }
        }

        if (userDTO.userName() != null) {
            oldUser.setUserName(userDTO.userName());
        }
        
        oldUser.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(oldUser);
        if (mobileChanged) {
            emailService.sendMobileNumberChangeNotification(
                oldUser.getEmailId(), 
                oldUser.getUserName(), 
                savedUser.getMobileNumber()
            );
        }

        return userMapper.toResponseDTO(savedUser);
    }
}