package com.arsh.project.secure_messaging.User;

import org.springframework.stereotype.Service;

//Service layer user service for management of users
@Service
public class UserService {


    private final UserRepo userRepository;

    public UserService(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    /*
     * Registers a new user.
     * Accepts user details and creates a new user account.
     * Will help hashing the password
     */
    public User registerUser(User user){
        return userRepository.save(user);
    }

    //Will be needed for messaging features and user lookup
    public User getUserByID(Integer id){
        return userRepository.findById(id)
                .orElse(null);//Returns null if the user is not found
    }

    public User getUserByEmail(String emailId){
        return userRepository.findByEmailId(emailId)
                .orElseThrow(()-> new RuntimeException("User not found with the emailID" + emailId));//Returns null if the user is not found
    }

    //users need an option to update profile
    public User updateUser(User updatedUser){
        //Retrieve the existing user details
        User oldUser = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found with the id" + updatedUser.getId()));

        //Update the required details
        if (updatedUser.getUserName()!=null)
            oldUser.setUserName(updatedUser.getUserName());
        if (updatedUser.getEmailId()!=null)
            oldUser.setEmailId(updatedUser.getEmailId());
        if (updatedUser.getMobileNo()!=null)
            oldUser.setMobileNo(updatedUser.getMobileNo());
        if (updatedUser.getPassword()!=null)
            oldUser.setPassword(updatedUser.getPassword());

        //Save the details to the old user
        return userRepository.save(oldUser);
    }


    //Removes the user from database for privacy and storage management
    public void deleteUser(Integer id){
        if (userRepository.existsById(id))//Finds if user is available for the given ID
            userRepository.deleteById(id);
        else
            throw new RuntimeException("User not found with ID: " + id);
    }


}
