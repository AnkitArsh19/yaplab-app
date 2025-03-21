package com.ankitarsh.securemessaging.Group;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * Creates a group with the given name in parameter.
     * @param createdById ID of the user creating the group.
     * @param groupName Name of the new group.
     * @return Response Entity of a created group.
     */
    @PostMapping("/create")
    public ResponseEntity<Groups> createGroup(
            @RequestParam Long createdById,
            @RequestParam String groupName
    ){
        return ResponseEntity.ok(groupService.createGroup(groupName, createdById));
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
