package com.yaplab.user;

import com.yaplab.enums.UserStatus;
import com.yaplab.security.authentication.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public UserService(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder, EmailService emailService, EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
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

        logger.info("User registered: {}", user.getEmailId());
        return userMapper.toRegisterResponseDTO(user);
    }

    /**
     * Sets the status of the user to offline.
     * This method is called when a user disconnects from the WebSocket.
     * @param userId The userId of the user to disconnect.
     * It updates the user's status to offline in the database.
     */
    public void disconnect(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.OFFLINE);
            userRepository.save(user);
            logger.info("User disconnected: {}", user.getEmailId());
        });
    }

    /**
     * Gets the user by the given ID from the database.
     * This method is used to fetch user details for various purposes.
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public UserResponseDTO getUserByID(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found");
                });
        return userMapper.toResponseDTO(user);
    }

    /**
     * Gets the user from the database using userId
     * @param id The userId of the user
     * @return The User object
     */
    public User getUserEntityByID(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found");
                });
    }

    /**
     * Finds the list of users with the searched set of characters
     * User can search by emailId or username or by mobile number.
     * This method is used to fetch user details for search functionality.
     * @param input the inputted set of characters
     * @return the list of User response DTO object
     */
    public List<UserResponseDTO> findUser(String input) {
        List<User> users = userRepository.findDistinctByUserNameIgnoreCaseOrEmailIdIgnoreCaseOrMobileNumber(
                input, input, input
        );
        logger.info("User search performed for input: {}", input);
        return users
                .stream()
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
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", emailId);
                    return new IllegalArgumentException("User not found with the emailId: " + emailId);
                });
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
        User oldUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userDTO.id());
                    return new IllegalArgumentException("User not found with the id" + userDTO.id());
                });

        if (userDTO.emailId() != null && !userDTO.emailId().equals(oldUser.getEmailId())) {
            userRepository.findByEmailId(userDTO.emailId())
                    .ifPresent(user -> {
                        logger.warn("Email already in use: {}", userDTO.emailId());
                        throw new IllegalArgumentException("Email - " + userDTO.emailId() + " already in use");
                    });
        }
        if (userDTO.userName() != null)
            oldUser.setUserName(userDTO.userName());
        if (userDTO.emailId() != null)
            oldUser.setEmailId(userDTO.emailId());
        if (userDTO.mobileNumber() != null)
            oldUser.setMobileNumber(userDTO.mobileNumber());
        User updatedUser = userMapper.toEntityFromDTO(userDTO);
        updatedUser.setId(oldUser.getId());
        updatedUser.setUpdatedAt(Instant.now());
        logger.info("User updated: {}", oldUser.getEmailId());
        return userMapper.toResponseDTO(userRepository.save(oldUser));
    }

    /**
     * Deletes the user from the database for privacy and storage management.
     * This method is used to remove a user from the system.
     * @param id ID of the user to delete.
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            logger.warn("Delete failed: User not found with ID: {}", id);
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        logger.info("User deleted with ID: {}", id);
    }

    /**
     * Finds the list of connected or disconnected users.
     * @param status status of the user
     * @return the list of user response DTO object.
     */
    public List<UserResponseDTO> findConnectedOrDisconnectedUsers(UserStatus status) {
        logger.info("Finding users with status: {}", status);
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
        String uploadsDir = "/uploads";
        String fileName = "profile_" + userId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadsDir, fileName);
        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            logger.error("Failed to store profile picture for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
        user.setProfilePictureUrl("/uploads/" + fileName);
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

        logger.info("Verification email resent to {}", user.getEmailId());
    }
}