package com.yaplab.user;

import com.yaplab.enums.UserStatus;
import com.yaplab.security.AppUserDetails;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for registering, finding, updating and deleting users.
 */
@Controller
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
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
    public ResponseEntity<?> updateDetails(
            @Valid @RequestBody UserDTO userDetails
    ){
        try {
            UserResponseDTO responseDTO = userService.updateUser(userDetails);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (duplicate mobile/email, invalid format, etc.)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during user update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred"));
        }
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
     * Uses authentication object to only get the users that have once logged in.
     * @param input Any set of characters that can be the email,name or mobile of the user
     */
    @GetMapping("/search/{input}")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(
            @PathVariable String input,
            Authentication authentication) {

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        List<UserResponseDTO> users = userService.findUser(input, currentUserId);
        return ResponseEntity.ok(users);
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
     * Gets comprehensive user status information for users in the current user's chat rooms.
     * @param authentication Authentication object to get current user ID
     * @return Map of user IDs to their status information
     */
    @GetMapping("/status/comprehensive")
    public ResponseEntity<Map<Long, Map<String, Object>>> getComprehensiveUserStatuses(
            Authentication authentication) {
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        Map<Long, Map<String, Object>> statuses = userService.getComprehensiveUserStatuses(userDetails.getId());
        return ResponseEntity.ok(statuses);
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

    /**
     * Initiates an email change request by sending a verification link to the new email.
     * @param request Contains the new email address and current password for verification
     * @return ResponseEntity with success message
     */
    @PostMapping("/initiate-email-change")
    public ResponseEntity<Map<String, String>> initiateEmailChange(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
            String newEmail = request.get("newEmail");
            String currentPassword = request.get("currentPassword");
            
            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New email is required"));
            }
            
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is required for verification"));
            }
            
            userService.initiateEmailChange(userDetails.getId(), newEmail.trim(), currentPassword);
            
            return ResponseEntity.ok(Map.of("message", "Verification email sent to " + newEmail + ". Please check your inbox to complete the email change."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}