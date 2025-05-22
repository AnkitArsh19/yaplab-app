package com.ankitarsh.securemessaging.user;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@RestController("/user")
public class UserController {

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @MessageMapping("/user.disconnectUser")
    @SendTo("/topic/status")
    public UserResponseDTO disconnectUser(
            @Payload Long userId){
        userService.disconnect(userId);
        return userService.getUserByID(userId);
    }

    /**
     * Finds user from id.
     * @param id ID of the user.
     * @return the user entity of that ID.
     */
    @GetMapping("/{id}")
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
    @GetMapping("/email")
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
    @PutMapping("/update")
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id){
        userService.disconnect(id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}