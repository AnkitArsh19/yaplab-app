package com.ankitarsh.securemessaging.User;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@RestController
public class UserController {

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    /**
     * Registers a new user.
     * @param newUserDetails Details of the user in a body.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/register")
    public User postNewUser(
            @Valid @RequestBody User newUserDetails){
        return this.userService.registerUser(newUserDetails);
    }

    /**
     * Login an existing user.
     * @param loginData Login data of the registered user.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/login")
    public User loginUser(@RequestBody Map<String, String> loginData) {
        String emailId = loginData.get("emailId");
        String loginPassword = loginData.get("password");
        return userService.loginUser(emailId, loginPassword);
    }

    /**
     * Finds user from id.
     * @param id ID of the user.
     * @return the user entity of that ID.
     */
    @GetMapping("/user/{id}")
    public User getUserFromId(
            @PathVariable Long id){
        return this.userService.getUserByID(id);
    }

    /**
     * Finds user by emailID.
     * @param email emailID of the user.
     * @return the user entity of that emailID.
     */
    @GetMapping("/user/email")
    public User getUserFromEmail(
            @RequestParam String email){
        return this.userService.getUserByEmail(email);
    }

    /**
     * Update user details with the details given in body.
     * @param userDetails details that needs to be updated
     * @return the User entity with updated details.
     */
    @PutMapping("/user/update")
    public User updateDetails(
            @Valid @RequestBody User userDetails
    ){
        return this.userService.updateUser(userDetails);
    }

    /**
     * Deletes a user from the database
     * @param id ID of the user
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}