package com.arsh.project.secure_messaging.User;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    /** Constructs a new UserController, injecting the UserService dependency.
     * The UserService handles user-related business logic, such as creating, retrieving,
     * updating, and deleting user accounts.**/
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    /*
     * Registers a new user.
     * Accepts user details in the request body and creates a new user account.
     * The user details to register (validated using @Valid).
     * Will help hashing the password
     */
    @PostMapping("/user/register")
    public User postUser(
            @Valid @RequestBody User userDetails){
        return this.userService.registerUser(userDetails);
    }

    @GetMapping("/user/{id}")
    public User getUserFromId(
            @PathVariable Long id){
        return this.userService.getUserByID(id);
    }

    @GetMapping("/user/email")
    public User getUserFromEmail(
            @RequestParam String email){
        return this.userService.getUserByEmail(email);
    }

    @PutMapping("/user/update")
    public User updateDetails(
            @Valid @RequestBody User userDetails
    ){
        return this.userService.updateUser(userDetails);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
