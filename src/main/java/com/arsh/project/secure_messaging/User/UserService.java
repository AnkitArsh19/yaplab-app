package com.arsh.project.secure_messaging.User;
import org.springframework.stereotype.Service;

/**
 * Service layer for handling user-related operations such as registration, finding user by id or email, update details, etc.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user and saves to the database.
     * @param user The user entity to register.
     * @return saved user.
     */
    public User registerUser(User user){

        return userRepository.save(user);
    }

    /**
     * Login a user to his existing account.
     * @param emailId The emailI of the user.
     * @param loginPassword The password of the user.
     * @return saved user.
     */
    public User loginUser(String emailId, String loginPassword){
        User activeUser = getUserByEmail(emailId);
        if(activeUser.getPassword().equals(loginPassword)){
            return activeUser;
        }
        else
            throw new RuntimeException("Please enter correct password");
    }

    /**
     * Gets the user by the given ID.
     * @param id ID of the user.
     * @return The User of that id or null if user is not found
     */
    public User getUserByID(Long id){
        return userRepository.findById(id)
                .orElse(null);
    }

    /**
     * Gets the user by the given emailID for recovery purpose.
     * @param emailId EmailId of the user.
     * @throws RuntimeException if user is not found.
     * @return the User found.
     */
    public User getUserByEmail(String emailId){
        return userRepository.findByEmailId(emailId)
                .orElseThrow(()-> new RuntimeException("User not found with the emailID" + emailId));
    }

    /**
     * Updates the required details of the user and saves in the database.
     * @param updatedUser The updated user to save details to.
     * @throws RuntimeException if user is not found.
     * @return the old user with updated details.
     */
    public User updateUser(User updatedUser){
        User oldUser = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found with the id" + updatedUser.getId()));

        if (updatedUser.getUserName()!=null)
            oldUser.setUserName(updatedUser.getUserName());
        if (updatedUser.getEmailId()!=null)
            oldUser.setEmailId(updatedUser.getEmailId());
        if (updatedUser.getMobileNumber()!=null)
            oldUser.setMobileNumber(updatedUser.getMobileNumber());
        if (updatedUser.getPassword()!=null)
            oldUser.setPassword(updatedUser.getPassword());

        return userRepository.save(oldUser);
    }


    /**
     * Deletes the user from the database for privacy and storage management.
     * @param id ID of the user to delete.
     * @throws RuntimeException if user is not found.
     */
    public void deleteUser(Long id){
        if (userRepository.existsById(id))
            userRepository.deleteById(id);
        else
            throw new RuntimeException("User not found with ID: " + id);
    }


}
