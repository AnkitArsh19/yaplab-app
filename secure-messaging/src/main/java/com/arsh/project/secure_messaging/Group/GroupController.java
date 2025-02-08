package com.arsh.project.secure_messaging.Group;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
public class GroupController {
    public final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<Groups> createGroup(
            @RequestParam Long createdById,
            @RequestParam String groupName
            ){
        return ResponseEntity.ok(groupService.createGroup(groupName, createdById));
    }

    @PostMapping("/adduser")
    public ResponseEntity<String> addUser(
            @RequestParam Long userId,
            @RequestParam Long groupId
    ){
        groupService.addUsers(userId, groupId);
        return ResponseEntity.ok("User added successfully.");
    }
}
