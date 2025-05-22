package com.ankitarsh.securemessaging.message;

import com.ankitarsh.securemessaging.enums.MessageStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller for handling messaging operations.
 * Provides endpoints for sending, retrieving, updating, and deleting messages.
 */
@Controller
@RestController
@RequestMapping("/message")
public class MessageController {

    /**
     * Constructor based dependency injection of Message Service, User Service, Group Service.
     */
    private final MessageService messageService;

    public MessageController(
            MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Sends a personal message between two users.
     * @return ResponseEntity indicating success or failure.
     */
    @MessageMapping("/personal")
    @GetMapping("/topic/messages")
    public MessageResponseDTO personalMessage(
            @Payload MessageDTO messageDTO
    ){
        return messageService.sendPersonalMessage(messageDTO);
    }
    /**
     * Sends a message in a group.
     * @return ResponseEntity indicating success or failure.
     */
    @MessageMapping("/group")
    public MessageResponseDTO groupMessage(
            @Payload MessageDTO messageDTO
    ){
        return messageService.sendGroupMessage(messageDTO);
    }

    /**
     * Returns list of messages between two users.
     *
     * @param senderId   ID of the sender.
     * @param receiverId ID of the receiver.
     * @return ResponseEntity with a list of messages or an empty entity.
     */
    @GetMapping("/personal/{senderId}/{receiverId}")
    public ResponseEntity<List<MessageResponseDTO>> getPersonalMessageList(
            @PathVariable Long senderId,
            @PathVariable Long receiverId
    ){
        List<MessageResponseDTO> messages = messageService.getMessageBetweenUsers(senderId, receiverId);
        if(messages.isEmpty()){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(messages);
    }

    /**
     * Returns list of messages between two users.
     *
     * @param groupId   ID of the group.
     * @return ResponseEntity with a list of messages or an empty entity.
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<MessageResponseDTO>> getGroupMessageList(
            @PathVariable Long groupId
    ){
        List<MessageResponseDTO> messages = messageService.getMessageOfGroups(groupId);
        if(messages.isEmpty()){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(messages);
    }

    /**
     * Updates the status of message to sent,read,delivered.
     *
     * @param messageId  ID of the message.
     * @param status    new status of the message.
     * @return ResponseEntity with a response of successful update.
     */
    @PatchMapping("/{messageId}/status/{status}")
    public ResponseEntity<String> updateMessageStatus(
            @PathVariable Long messageId,
            @RequestParam MessageStatus status
    ){
        messageService.updateMessageStatus(messageId, status);
        return ResponseEntity.ok("Status Updated to " + status);
    }

    /**
     * Soft deletes a message.
     * @param messageId ID of the message.
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId
    ){
        messageService.softDeleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
