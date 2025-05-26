package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling messaging operations.
 * Provides endpoints for sending, retrieving, updating, and deleting messages.
 */
@Controller
@RequestMapping("/messages")
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
     * Handles incoming WebSocket messages to update a message's status to DELIVERED.
     * @return A map containing the messageId and the new status.
     */
    @MessageMapping("/status/delivered")
    @SendTo("/topic/messages") // Or a chatroom-specific topic
    public Map<String, Object> handleDeliveredStatusUpdate(@Payload Long messageId) {
        messageService.updateMessageStatus(messageId, MessageStatus.DELIVERED);
        return createStatusUpdatePayload(messageId, MessageStatus.DELIVERED);
    }
    /**
     * Handles incoming WebSocket messages to update a message's status to READ.
     * @return A map containing the messageId and the new status.
     */
    @MessageMapping("/status/read")
    @SendTo("/topic/messages") // Or a chatroom-specific topic
    public Map<String, Object> handleReadStatusUpdate(@Payload Long messageId) {
        messageService.updateMessageStatus(messageId, MessageStatus.READ);
        return createStatusUpdatePayload(messageId, MessageStatus.READ);
    }

    /**
     * This method sends a personal message between two users.
     * @param messageDTO The DTO containing message details such as sender, receiver, content, etc.
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
     * This method sends a message in a group.
     * @param messageDTO The DTO containing message details such as sender, group ID, content, etc.
     * @return ResponseEntity indicating success or failure.
     */
    @MessageMapping("/group")
    public MessageResponseDTO groupMessage(
            @Payload MessageDTO messageDTO
    ){
        return messageService.sendGroupMessage(messageDTO);
    }

    /**
     * Sends a reply message to an existing message via WebSocket.
     * @return The created MessageResponseDTO.
     */
    @MessageMapping("/reply")
    @SendTo("/topic/messages")
    public MessageResponseDTO sendReplyMessageViaWebSocket(
            @Payload MessageDTO messageDTO) {
        return messageService.sendReplyMessage(messageDTO, messageDTO.repliedToMessageId());
    }

    /**
     * Sends a reply message to an existing message.
     * @return ResponseEntity with the created MessageResponseDTO.
     */
    @PostMapping("/reply")
    public ResponseEntity<MessageResponseDTO> sendReplyMessage(
            @RequestBody MessageDTO messageDTO) {
        return ResponseEntity.ok(messageService.sendReplyMessage(messageDTO, messageDTO.repliedToMessageId()));
    }

    /**
     * This method retrieves messages sent between two users.
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
     * This method retrieves messages sent in a group.
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
     * This method is used to update the status of a message based on its ID.
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
     * This method soft deletes a message.
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

    private Map<String, Object> createStatusUpdatePayload(Long messageId, MessageStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("status", status);
        return payload;
    }
}
