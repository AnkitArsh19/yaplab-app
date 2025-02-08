package com.arsh.project.secure_messaging.Message;

import com.arsh.project.secure_messaging.Group.GroupService;
import com.arsh.project.secure_messaging.Group.Groups;
import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.User.UserService;
import com.arsh.project.secure_messaging.enums.MessageStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {
    private final MessageService messageService;
    private final UserService userService;
    private final GroupService groupService;

    public MessageController(MessageService messageService, UserService userService, GroupService groupService) {
        this.messageService = messageService;
        this.userService = userService;
        this.groupService = groupService;
    }

    @PostMapping("/personal")
    public ResponseEntity<String> personalMessage(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam String content
    ){
        User sender = userService.getUserByID(senderId);
        User receiver = userService.getUserByID(receiverId);
        if(sender == null || receiver == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        messageService.sendPersonalMessage(sender, receiver, content);
        return ResponseEntity.ok("The message was sent.");
    }

    @PostMapping("/group")
    public ResponseEntity<String> groupMessage(
            @RequestParam Long senderId,
            @RequestParam Long groupId,
            @RequestParam String content
    ){
        User sender = userService.getUserByID(senderId);
        Groups group = groupService.getGroupById(groupId);
        if(!group.getUsers().contains(sender)) {
            throw new RuntimeException("User is not the member of this group");
        }
        messageService.sendGroupMessage(sender, group, content);
        return ResponseEntity.ok("The message was sent to the group.");
    }

    @GetMapping("/personal/{senderId}/{receiverId}")
    public ResponseEntity<List<Message>> getPersonalMessageList(
            @PathVariable Long senderId,
            @PathVariable Long receiverId
    ){
        User sender = userService.getUserByID(senderId);
        User receiver = userService.getUserByID(receiverId);
        List<Message> messages = messageService.getMessageBetweenUsers(sender, receiver);
        if(messages.isEmpty()){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Message>> getGroupMessageList(
            @PathVariable Long groupId
    ){
        Groups group = groupService.getGroupById(groupId);
        List<Message> messages = messageService.getMessageOfGroups(group);
        if(messages.isEmpty()){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/{messageId}/status/{status}")
    public ResponseEntity<String> updateMessageStatus(
            @PathVariable Long messageId,
            @RequestParam MessageStatus status
            ){
        messageService.updateMessageStatus(messageId, status);
        return ResponseEntity.ok("Status Updated to " + status);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId
    ){
        messageService.softDeleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
