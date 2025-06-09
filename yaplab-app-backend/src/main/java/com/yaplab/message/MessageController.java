package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling messaging operations via REST and WebSocket.
 */
@Controller
@RequestMapping("/messages")
public class MessageController {

    /**
     * Constructor based dependency injection
     */
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles incoming WebSocket messages to update a message's status to DELIVERED.
     * Sends this to the topic so people subscribed to it also get notified.
     * @param messageId ID of the message
     */
    @MessageMapping("/status/delivered")
    public void handleDeliveredStatusUpdate(@Payload Long messageId) {
        messageService.updateMessageStatus(messageId, MessageStatus.DELIVERED);
        messagingTemplate.convertAndSend("/topic/messages/status", createStatusUpdatePayload(messageId, MessageStatus.DELIVERED));
    }

    /**
     * Handles incoming WebSocket messages to update a message's status to READ.
     * Sends this to the topic so people subscribed to it also get notified.
     * @param messageId ID of the message
     */
    @MessageMapping("/status/read")
    public void handleReadStatusUpdate(@Payload Long messageId) {
        messageService.updateMessageStatus(messageId, MessageStatus.READ);
        messagingTemplate.convertAndSend("/topic/messages/status", createStatusUpdatePayload(messageId, MessageStatus.READ));
    }

    /**
     * Sends a personal message via WebSocket to the appropriate chatroom topic.
     * @param messageDTO the message DTO coming from the client.
     */
    @MessageMapping("/personal")
    public void personalMessage(@Payload MessageDTO messageDTO) {
        MessageResponseDTO response = messageService.sendPersonalMessage(messageDTO);
        String chatroomId = response.chatRoomId();
        messagingTemplate.convertAndSend("/topic/chat/" + chatroomId, response);
    }

    /**
     * Sends a group message via WebSocket to the appropriate chatroom topic.
     * @param messageDTO the message DTO coming from the client.
     */
    @MessageMapping("/group")
    public void groupMessage(@Payload MessageDTO messageDTO) {
        MessageResponseDTO response = messageService.sendGroupMessage(messageDTO);
        String chatroomId = response.chatRoomId(); // Ensure your DTO has this field
        messagingTemplate.convertAndSend("/topic/chat/" + chatroomId, response);
    }

    /**
     * Sends a reply message via WebSocket to the appropriate chatroom topic.
     * @param messageDTO the message DTO coming from the client.
     */
    @MessageMapping("/reply")
    public void sendReplyMessageViaWebSocket(@Payload MessageDTO messageDTO) {
        MessageResponseDTO response = messageService.sendReplyMessage(messageDTO, messageDTO.repliedToMessageId());
        String chatroomId = response.chatRoomId();
        messagingTemplate.convertAndSend("/topic/chat/" + chatroomId, response);
    }

    /**
     * Sends a reply message to an existing message.
     * @param messageDTO the message DTO coming from the client.
     */
    @PostMapping("/reply")
    public ResponseEntity<MessageResponseDTO> sendReplyMessage(
            @RequestBody MessageDTO messageDTO) {
        return ResponseEntity.ok(messageService.sendReplyMessage(messageDTO, messageDTO.repliedToMessageId()));
    }

    /**
     * Updates the status of a message based on its ID.
     */
    @PatchMapping("/{messageId}/status/{status}")
    public ResponseEntity<String> updateMessageStatus(
            @PathVariable Long messageId,
            @PathVariable MessageStatus status
    ){
        messageService.updateMessageStatus(messageId, status);
        return ResponseEntity.ok("Status Updated to " + status);
    }

    /**
     * Soft deletes a message.
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> softDeleteMessage(
            @PathVariable Long messageId, // Message ID from path
            @RequestParam Long userId // User ID from request parameter
    ){
        messageService.softDeleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a payload for message status updates.
     * @param messageId ID of the message
     * @param status Status of the message
     * @return Map containing the message ID and status
     */
    private Map<String, Object> createStatusUpdatePayload(Long messageId, MessageStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        payload.put("status", status);
        return payload;
    }

    /**
     * Edits an existing message.
     */
    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent) {
        Message updatedMessage = messageService.editMessage(messageId, newContent);
        return ResponseEntity.ok(updatedMessage);
    }

    /**
     * Forwards an existing message.
     */
    @PostMapping("/{messageId}/forward")
    public ResponseEntity<Message> forwardMessage(
            @PathVariable Long messageId,
            @RequestBody Map<String, Object> requestBody) {
        String recipientChatRoomId = (String) requestBody.get("recipientChatRoomId");
        Long senderId = (Long) requestBody.get("senderId");
        Message forwardedMessage = messageService.forwardMessage(messageId, recipientChatRoomId, senderId); // Pass recipientChatRoomId as String
        return ResponseEntity.ok(forwardedMessage);
    }
}