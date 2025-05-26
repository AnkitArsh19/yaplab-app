package com.yaplab.user;

import jakarta.validation.Valid;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Objects;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@Controller
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
     * This method is called when a user disconnects from the WebSocket.
     * @param userId ID of the user to disconnect.
     * @return UserResponseDTO containing the updated user details.
     */
    @MessageMapping("/user.disconnectUser")
    @SendTo("/topic/status")
    public UserResponseDTO disconnectUser(
            @Payload Long userId){
        userService.disconnect(userId);
        return userService.getUserByID(userId);
    }

    /**
     * This method retrieves a user by their ID.
     * It returns a ResponseEntity containing the UserResponseDTO with the user's details.
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
     * This method retrieves a user by their email ID.
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
     * This method is used to update the user details such as username, email, mobile number, etc.
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
     * This method is used to delete a user by their ID.
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

    /**
     * Searches the list of entire users in the database to provide in the search box.
     * @param input Any set of characters that can be the email,name or mobile of the user
     */
    @GetMapping("/search/{input}")
    public ResponseEntity<List<UserResponseDTO>> findUsers(
            @PathVariable String input
    ){
        List<UserResponseDTO> response = userService.findUser(input);
        return ResponseEntity.ok(response);
    }

    /**
     * This method is used to handle WebSocket disconnect events.
     * It listens for SessionDisconnectEvent and updates the user's status to offline.
     * @param event The SessionDisconnectEvent containing the session attributes.
     */
    @EventListener
    public void handleWebSocketDisconnectListener
            (SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        if(userId != null){
            userService.disconnect(Long.valueOf(userId));
        }
    }

    /**
     * This method is used to upload a new profile picture for the user.
     * @param id UserId of the user
     * @param file File uploaded by the user
     */
    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<Void> updateProfilePicture(
            @PathVariable Long id,
            @RequestParam("file")MultipartFile file
            ){
        userService.updateProfilePicture(id, file);
        return ResponseEntity.ok().build();
    }
}