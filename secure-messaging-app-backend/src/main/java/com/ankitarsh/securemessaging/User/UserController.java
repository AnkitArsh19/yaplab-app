package com.ankitarsh.securemessaging.User;

import com.ankitarsh.securemessaging.Authentication.LoginRequestDTO;
import com.ankitarsh.securemessaging.Authentication.LoginResponseDTO;
import com.ankitarsh.securemessaging.Authentication.RegisterRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@Controller
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

    @MessageMapping("/user.addUser")
    @SendTo("/user/topic")
    public UserResponseDTO postNewUser(
            @Payload RegisterRequestDTO newUserDetails){
        return this.userService.registerUser(newUserDetails);
    }


    @MessageMapping("/user.disconnectUser")
    @SendTo("/user/topic")
    public Void disconnectUser(
            @Payload Long userId){
        UserResponseDTO user = userService.getUserByID(userId);
        userService.disconnect(userId);
        return null;
    }

    /**
     * Login an existing user.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(
            @RequestBody LoginRequestDTO loginRequest) {
        String emailId = loginRequest.emailId();
        String loginPassword = loginRequest.password();
        LoginResponseDTO response = userService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Finds user from id.
     * @param id ID of the user.
     * @return the user entity of that ID.
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<UserResponseDTO> getUserFromId(
            @PathVariable Long id){
        UserResponseDTO responseDTO = userService.getUserByID(id);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Finds user by emailId.
     * @param email emailId of the user.
     * @return the user entity of that emailId.
     */
    @GetMapping("/user/email")
    public ResponseEntity<UserResponseDTO> getUserFromEmail(
            @RequestParam String email){
        UserResponseDTO responseDTO = userService.getUserByEmail(email);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Update user details with the details given in body.
     * @param userDetails details that needs to be updated
     * @return the User entity with updated details.
     */
    @PutMapping("/user/update")
    public ResponseEntity<UserResponseDTO> updateDetails(
            @Valid @RequestBody UserDTO userDetails
    ){
       UserResponseDTO responseDTO = userService.updateUser(userDetails);
       return ResponseEntity.ok(responseDTO);
    }

    /**
     * Deletes a user from the database
     * @param id ID of the user
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id){
        userService.disconnect(id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}