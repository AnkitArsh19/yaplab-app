package com.yaplab.user;

import com.yaplab.security.authentication.*;
import com.yaplab.enums.UserStatus;
import com.resend.core.exception.ResendException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for handling user-related operations such as registration, finding user by id or email, update details, etc.
 */
@Service
public class UserService {

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
     * Does not register an existing user in the database.
     * Password is encoded and saved in the database using password encoder.
     * @param registerRequestDTO A DTO with fields given by user to register
     * @throws ResendException when email could not be sent after registration
     * @return A responseDTO sent in response
     */
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) throws ResendException {
        User user = userMapper.toEntityFromRegisterRequest(registerRequestDTO);
        Optional<User> userExists = userRepository.findByEmailId(user.getEmailId());
        if (userExists.isPresent()) {
            throw new IllegalArgumentException("User already exists with the given email id");
        }
        user.setStatus(UserStatus.OFFLINE);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(86400); // Example: Token valid for 24 hours
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(expiry);
        verificationToken.setUser(user);
        emailVerificationTokenRepository.save(verificationToken);
        String verificationLink = "http://localhost:8080/verify-email?token=" + token; // Update URL as needed
        emailService.sendVerificationEmail(user.getEmailId(), verificationLink);
        emailService.sendWelcomeEmail(user.getEmailId(), user.getUserName());
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
        });
    }

    /**
     * Gets the user by the given ID from the database.
     * This method is used to fetch user details for various purposes.
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public UserResponseDTO getUserByID(Long id) {
        return userMapper.toResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    /**
     * Gets the user from the database using userId
     * @param id The userId of the user
     * @return The User object
     */
    public User getUserEntityByID(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
        return users
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets the user by the given emailId for recovery purpose.
     * @param emailId EmailId of the user.
     * @return the User found.
     * @throws IllegalArgumentException if user is not found.
     */
    public UserResponseDTO getUserByEmail(String emailId) {
        return userMapper.toResponseDTO(userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the emailId: " + emailId)));
    }

    /**
     * Updates the required details of the user and saves in the database.
     * This method is used to update user details such as username, emailId, and mobile number.
     * @param userDTO The updated user to save details to.
     * @return the old user with updated details.
     * @throws IllegalArgumentException if user is not found or email already in use.
     */
    public UserResponseDTO updateUser(UserDTO userDTO) {
        User oldUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the id" + userDTO.id()));

        if (userDTO.emailId() != null && !userDTO.emailId().equals(oldUser.getEmailId())) {
            userRepository.findByEmailId(userDTO.emailId())
                    .ifPresent(user -> {
                        throw new IllegalArgumentException("Email - " + userDTO.emailId() + "already in use");
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
        return userMapper.toResponseDTO(userRepository.save(oldUser));
    }


    /**
     * Deletes the user from the database for privacy and storage management.
     * This method is used to remove a user from the system.
     * @param id ID of the user to delete.
     * @throws IllegalArgumentException if user is not found.
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);

    }

    /**
     * Finds the list of connected or disconnected users.
     * @param status status of the user
     * @return the list of user response DTO object.
     */
    public List<UserResponseDTO> findConnectedUsers(UserStatus status) {
        return userRepository.findByStatus(status)
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates the profile picture url with the file stored in the "uploads" folder.
     * Checks that the file size does not exceed 5MB and are of type png/jpeg/jpg.
     * Maintains consistent naming of all pictures.
     * @param userId The user id of the person who wants to update profile picture.
     * @param file The file uploaded by the user.
     */
    public void updateProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg") ||
                        contentType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("Only JPEG, JPG, and PNG files are allowed.");
        }
        long maxSize = 5 * 1024 * 1024; // 5MB

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 5MB.");
        }
        String uploadsDir = "/uploads";
        String fileName = "profile_" + userId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadsDir, fileName);
        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        user.setProfilePictureUrl("/uploads/" + fileName);
        userRepository.save(user);
    }

    /**
     * Gets the creation date of the user.
     * @param userId The ID of the user.
     * @return The Instant representing the creation date.
     */
    public Instant getUserCreationDate(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getCreatedAt();
    }

    /**
     * Gets the last update date of the user.
     * @param userId The ID of the user.
     * @return The Instant representing the last update date.
     */
    public Instant getUserLastUpdateDate(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getUpdatedAt();
    }

}