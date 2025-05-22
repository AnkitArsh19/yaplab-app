package com.ankitarsh.securemessaging.group;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
