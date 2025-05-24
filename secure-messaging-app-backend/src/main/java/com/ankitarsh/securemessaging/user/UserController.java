package com.ankitarsh.securemessaging.user;

import jakarta.validation.Valid;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Objects;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Disconnects the user after doing a logout
     * @param userId
     * @return
     */
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

    @GetMapping("/search/{userName}")
    public ResponseEntity<List<UserResponseDTO>> findUsers(
            @PathVariable String userName
    ){
        List<UserResponseDTO> response = userService.findUsername(userName);
        return ResponseEntity.ok(response);
    }

    @EventListener
    public void handleWebSocketDisconnectListener
            (SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        if(userId != null){
            userService.disconnect(Long.valueOf(userId));
        }
    }
}