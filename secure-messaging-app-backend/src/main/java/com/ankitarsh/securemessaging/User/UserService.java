package com.ankitarsh.securemessaging.User;
import com.ankitarsh.securemessaging.Authentication.LoginRequestDTO;
import com.ankitarsh.securemessaging.Authentication.LoginResponseDTO;
import com.ankitarsh.securemessaging.Authentication.RegisterRequestDTO;
import com.ankitarsh.securemessaging.enums.UserStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for handling user-related operations such as registration, finding user by id or email, update details, etc.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Registers a new user and saves to the database.
     * @return saved user.
     */
    public UserResponseDTO registerUser(RegisterRequestDTO registerRequestDTO){
        User user = userMapper.toEntityFromRegisterRequest(registerRequestDTO);
        Optional<User> userExists = userRepository.findByEmailId(user.getEmailId());
        if(userExists.isPresent()) {
            throw new IllegalArgumentException("User already exists with the given email id");
        }
        user.setStatus(UserStatus.OFFLINE);
        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    /**
     * Login a user to his existing account.
     * @return saved user.
     */
    public LoginResponseDTO loginUser(LoginRequestDTO loginRequestDTO){
        User user = userRepository.findByEmailId(loginRequestDTO.emailId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(!user.getPassword().equals(loginRequestDTO.password())) {
            throw new IllegalArgumentException("Please enter correct password");
        }
        user.setStatus(UserStatus.ONLINE);
        return userMapper.toLoginResponseDTO(user);
    }

    public void disconnect(Long userId){
        userRepository.findById(userId).ifPresent(user -> {
                user.setStatus(UserStatus.OFFLINE);
                userRepository.save(user);
        });
    }

    /**
     * Gets the user by the given ID.
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public UserResponseDTO getUserByID(Long id){
        return userMapper.toResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    public User getUserEntityByID(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Gets the user by the given emailId for recovery purpose.
     * @param emailId EmailId of the user.
     * @throws RuntimeException if user is not found.
     * @return the User found.
     */
    public UserResponseDTO getUserByEmail(String emailId){
        return userMapper.toResponseDTO(userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the emailId: " + emailId)));
    }

    /**
     * Updates the required details of the user and saves in the database.
     * @param userDTO The updated user to save details to.
     * @throws RuntimeException if user is not found.
     * @return the old user with updated details.
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
        if (userDTO.userName()!=null)
            oldUser.setUserName(userDTO.userName());
        if (userDTO.emailId()!=null)
            oldUser.setEmailId(userDTO.emailId());
        if (userDTO.mobileNumber()!=null)
            oldUser.setMobileNumber(userDTO.mobileNumber());
        if (userDTO.password()!=null)
            oldUser.setPassword(userDTO.password());
        User updatedUser = userMapper.toEntityFromDTO(userDTO);
        updatedUser.setId(oldUser.getId());

        return userMapper.toResponseDTO(userRepository.save(oldUser));
    }


    /**
     * Deletes the user from the database for privacy and storage management.
     * @param id ID of the user to delete.
     * @throws RuntimeException if user is not found.
     */
    public void deleteUser(Long id){
        if (!userRepository.existsById(id)){
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);

    }

    public List<UserResponseDTO> findConnectedUsers(){
        return userRepository.findByStatus(UserStatus.ONLINE)
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

}