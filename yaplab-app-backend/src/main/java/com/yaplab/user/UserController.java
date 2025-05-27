package com.yaplab.user;

import com.yaplab.enums.UserStatus;
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

import java.time.Instant;
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
     * Payload expects a user id.
     * It is then broadcasted to every member of /topic/status that the user is diconnected.
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
     * This method is used to handle WebSocket disconnect events which can be unintentional.
     * Header accessor contains metadata of the disconnect event.
     * If session attributes are not null, get the user ID.
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

    /**
     * Finds a list of connected or disconnected users.
     * @param status the status to get the list
     * @return a list of userResponseDTO
     */
    @GetMapping("/list/{status}")
    public ResponseEntity<List<UserResponseDTO>> findConnectedOrDisconnectedUsers(
            @PathVariable UserStatus status
            ){
        List<UserResponseDTO> list = userService.findConnectedOrDisconnectedUsers(status);
        return ResponseEntity.ok(list);
    }

    /**
     * This method retrieves the creation date of a user by their ID.
     * @param id ID of the user.
     * @return The Instant representing the creation date.
     */
    @GetMapping("/{id}/creation-date")
    public ResponseEntity<Instant> getUserCreationDate(@PathVariable Long id) {
        Instant creationDate = userService.getUserCreationDate(id);
        return ResponseEntity.ok(creationDate);
    }

    /**
     * This method retrieves the last update date of a user by their ID.
     * @param id ID of the user.
     * @return The Instant representing the last update date.
     */
    @GetMapping("/{id}/last-update-date")
    public ResponseEntity<Instant> getUserLastUpdateDate(@PathVariable Long id) {
        Instant lastUpdateDate = userService.getUserLastUpdateDate(id);
        return ResponseEntity.ok(lastUpdateDate);
    }
}