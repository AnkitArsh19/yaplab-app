package com.yaplab.user;

import com.yaplab.authentication.RegisterRequestDTO;
import com.yaplab.authentication.RegisterResponseDTO;
import com.yaplab.enums.UserStatus;
import com.resend.core.exception.ResendException;
import com.yaplab.authentication.EmailService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
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

    public UserService(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registers a new user and saves to the database.
     * Does not register an existing user in the database.
     * Password is encoded and saved in the database using password encoder.
     *
     * @param registerRequestDTO A DTO with fields given by user to register
     * @return A responseDTO sent in response
     */
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) throws ResendException {
        User user = userMapper.toEntityFromRegisterRequest(registerRequestDTO);
        Optional<User> userExists = userRepository.findByEmailId(user.getEmailId());
        if (userExists.isPresent()) {
            throw new IllegalArgumentException("User already exists with the given email id");
        }
        user.setStatus(UserStatus.OFFLINE);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmailId(), user.getUserName());
        return userMapper.toRegisterResponseDTO(user);
    }

    /**
     * Sets the status of the user to offline.
     *
     * @param userId The userId of the userId
     */
    public void disconnect(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.OFFLINE);
            userRepository.save(user);
        });
    }

    /**
     * Gets the user by the given ID.
     *
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public UserResponseDTO getUserByID(Long id) {
        return userMapper.toResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    /**
     * Gets the user from the database using userId
     *
     * @param id The userId of the user
     * @return The User object
     */
    public User getUserEntityByID(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Finds the list of users with the searched set of characters
     *
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
     *
     * @param emailId EmailId of the user.
     * @return the User found.
     * @throws RuntimeException if user is not found.
     */
    public UserResponseDTO getUserByEmail(String emailId) {
        return userMapper.toResponseDTO(userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the emailId: " + emailId)));
    }

    /**
     * Updates the required details of the user and saves in the database.
     *
     * @param userDTO The updated user to save details to.
     * @return the old user with updated details.
     * @throws RuntimeException if user is not found.
     */
    public UserResponseDTO updateUser(UserDTO userDTO) {
        User oldUser = userRepository.findById(userDTO.id())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the id" + userDTO.id()));

        if (userDTO.emailId() != null && !userDTO.emailId().equals(oldUser.getEmailId())) {
            userRepository.findByEmailId(userDTO.emailId())
                    .ifPresent(user -> {
                        throw new IllegalArgumentException("Email already in use");
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

        return userMapper.toResponseDTO(userRepository.save(oldUser));
    }


    /**
     * Deletes the user from the database for privacy and storage management.
     *
     * @param id ID of the user to delete.
     * @throws RuntimeException if user is not found.
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);

    }

    /**
     * Finds the list of connected or disconnected users.
     *
     * @param status status of the user
     * @return the list of user response DTO object.
     */
    public List<UserResponseDTO> findConnectedUsers(UserStatus status) {
        return userRepository.findByStatus(status)
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

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

}