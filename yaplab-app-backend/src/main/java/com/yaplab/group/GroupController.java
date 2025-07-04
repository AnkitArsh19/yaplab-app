package com.yaplab.group;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for handling group operations.
 * Provides endpoints for creating groups and adding users.
 */
@RestController
@RequestMapping("/groups")
public class GroupController {

    /**
     * Constructor based dependency injection of Group Service.
     */
    public final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * Creates a group with the given userName in parameter.
     * @param createdById ID of the user creating the group.
     * @return Response Entity of a created group.
     */
    @PostMapping("/create")
    public ResponseEntity<GroupResponseDTO> createGroup(
            @RequestBody GroupDTO groupDTO,
            @RequestParam Long createdById
    ){
        return ResponseEntity.ok(groupService.createGroup(groupDTO, createdById));
    }

    /**
     * Adds a new user to the group entity.
     * @param groupId ID of the group.
     * @param userId  ID of the user.
     * @return Response Entity indicating successful addition.
     */
    @PostMapping("/adduser")
    public ResponseEntity<String> addUser(
            @RequestParam Long userId,
            @RequestParam Long groupId
    ){
        groupService.addUsers(userId, groupId);
        return ResponseEntity.ok("User added successfully.");
    }

    /**
     * Removes a user from the group entity.
     * @param groupId ID of the group.
     * @param userId  ID of the user to remove.
     * @return Response Entity indicating successful removal.
     */
    @DeleteMapping("/removeuser")
    public ResponseEntity<String> removeUser(
            @RequestParam Long userId,
            @RequestParam Long groupId
    ){
        groupService.removeUser(userId, groupId);
        return ResponseEntity.ok("User removed successfully.");
    }

    /**
     * This method is used to upload a new profile picture for the group.
     * @param id GroupId of the group
     * @param file File uploaded by the user
     */
    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<Void> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ){
        groupService.updateProfilePicture(id, file);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets detailed information about a group including creator, members, and metadata.
     * @param id ID of the group.
     * @return Response Entity containing detailed group information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> getGroupDetails(@PathVariable Long id) {
        GroupResponseDTO groupDetails = groupService.getGroupDetails(id);
        return ResponseEntity.ok(groupDetails);
    }

    /**
     * Deletes a group. Only the group creator can delete the group.
     * @param id ID of the group to delete.
     * @param userId ID of the user attempting to delete (must be creator).
     * @return Response Entity indicating successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGroup(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        groupService.deleteGroup(id, userId);
        return ResponseEntity.ok("Group deleted successfully.");
    }

    /**
     * Updates the group name. Only the group creator can update.
     * @param id Group ID to update
     * @param userId ID of the user making the change (must be creator)
     * @param groupUpdateDTO The new group data
     * @return Updated group response
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponseDTO> updateGroup(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody GroupUpdateDTO groupUpdateDTO
    ) {
        GroupResponseDTO updatedGroup = groupService.updateGroupName(id, groupUpdateDTO, userId);
        return ResponseEntity.ok(updatedGroup);
    }
}
