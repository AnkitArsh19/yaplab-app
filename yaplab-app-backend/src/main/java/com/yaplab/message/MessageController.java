package com.yaplab.message;

import com.yaplab.config.WebSocketConnectionManager;
import com.yaplab.enums.MessageStatus;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced controller for real-time messaging operations
 */
@Controller
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketConnectionManager connectionManager;
    private final UserService userService;
    private final MessageStatusService messageStatusService;

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate,
                             WebSocketConnectionManager connectionManager, UserService userService, MessageStatusService messageStatusService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.connectionManager = connectionManager;
        this.userService = userService;
        this.messageStatusService = messageStatusService;
    }

    /**
     * Handles sending personal messages.
     * This method processes a personal message sent by a user and broadcasts it to the appropriate chat room.
     * It also sends a confirmation to the sender and updates the message status for the recipient.
     * Broadcasts the message to the chat room and to the recipient if they are online.
     * @param messageDTO The message data transfer object containing message details.
     * @param principal The authenticated user principal.
     */
    @MessageMapping("/messages/personal")
    public void sendPersonalMessage(@Payload MessageDTO messageDTO, Principal principal) {
        try {
            MessageResponseDTO response = messageService.sendPersonalMessage(messageDTO);
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("id", response.id());
            broadcastMessage.put("content", response.content());
            broadcastMessage.put("senderId", messageDTO.senderId());
            broadcastMessage.put("senderName", response.senderName());
            broadcastMessage.put("chatRoomId", response.chatRoomId());
            broadcastMessage.put("timestamp", response.timestamp().toString());
            broadcastMessage.put("messageStatus", "SENT");
            broadcastMessage.put("chatRoomType", "PERSONAL");
            broadcastMessage.put("edited", response.edited());
            broadcastMessage.put("forwarded", response.forwarded());
            broadcastMessage.put("editTimestamp", response.editTimestamp() != null ? response.editTimestamp().toString() : null);
            broadcastMessage.put("repliedToMessage", response.repliedToMessage());

            if (response.fileUrl() != null) {
                broadcastMessage.put("fileUrl", response.fileUrl());
                broadcastMessage.put("fileName", response.fileName());
                broadcastMessage.put("fileSize", response.fileSize());
                broadcastMessage.put("fileType", response.fileType());
                broadcastMessage.put("uploadedByUserId", response.uploadedByUserId());
                broadcastMessage.put("uploadedByUserName", response.uploadedByUserName());
            }

            messagingTemplate.convertAndSend("/room/" + response.chatRoomId() + "/messages", broadcastMessage);
            if (messageDTO.receiverId() != null) {
                messagingTemplate.convertAndSendToUser(
                    messageDTO.receiverId().toString(),
                    "/messages",
                    broadcastMessage
                );
                boolean isRecipientOnline = connectionManager.isUserOnline(messageDTO.receiverId());

                if (isRecipientOnline) {
                    messageStatusService.markUndeliveredMessagesAsDelivered(
                            response.chatRoomId(),
                            messageDTO.receiverId()
                    );
              }
            }

        } catch (Exception e) {
            logger.error("Error sending personal message: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            );
            messagingTemplate.convertAndSendToUser(
                    messageDTO.senderId().toString(),
                    "/status",
                    errorResponse
            );
        }
    }

    /**
     * Handles sending group messages.
     * This method processes a group message sent by a user and broadcasts it to the appropriate chat room.
     * It also sends a confirmation to the sender and updates the message status for all recipients.
     * @param messageDTO The message data transfer object containing message details.
     * @param principal The authenticated user principal.
     */
    @MessageMapping("/messages/group")
    public void sendGroupMessage(@Payload MessageDTO messageDTO, Principal principal) {
        try {

            MessageResponseDTO response = messageService.sendGroupMessage(messageDTO);

            // Broadcast to room
            messagingTemplate.convertAndSend("/room/" + response.chatRoomId() + "/messages", response);

            // Send confirmation to sender
            messagingTemplate.convertAndSendToUser(
                    messageDTO.senderId().toString(),
                    "/status",
                    Map.of("messageId", response.id(), "status", "SENT")
            );
        } catch (Exception e) {
            logger.error("Error sending group message", e);
            messagingTemplate.convertAndSendToUser(
                    messageDTO.senderId().toString(),
                    "/status",
                    Map.of("status", "ERROR", "error", e.getMessage())
            );
        }
    }

    /**
     * Mark messages as read.
     * @param request The request containing message IDs to mark as read
     */
    @MessageMapping("/messages.read")
    public void markMessagesAsRead(@Payload MessageStatusUpdateRequest request) {
        messageStatusService.updateMessageStatus(
                request.chatroomId(),
                request.userId(),
                request.messageIds(),
                MessageStatus.READ
        );
    }

    /**
     * Handle delivery status updates
     */
    @MessageMapping("/messages.delivered")
    public void handleDeliveryStatus(@Payload MessageStatusUpdateRequest request) {
        messageStatusService.updateMessageStatus(
                request.chatroomId(),
                request.userId(),
                request.messageIds(),
                MessageStatus.DELIVERED
        );
    }

    /**
     * Handle typing indicator for a specific chat room.
     * @param roomId The ID of the chat room where the typing indicator is sent.
     * @param principal The authenticated user principal who is typing.
     */
    @MessageMapping("/messages/typing/{roomId}")
    public void handleTyping(@DestinationVariable String roomId, Principal principal) {
        try {
            User user = userService.getUserEntityByEmail(principal.getName());
            messagingTemplate.convertAndSend("/room/" + roomId + "/events",
                    Map.of("type", "TYPING", "userId", user.getId(), "userName", user.getUserName(), "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            logger.error("Error handling typing indicator", e);
        }
    }

    /**
     * Handle stop typing indicator for a specific chat room.
     * @param roomId The ID of the chat room where the stop typing indicator is sent.
     * @param principal The authenticated user principal who stopped typing.
     */
    @MessageMapping("/messages/stop-typing/{roomId}")
    public void handleStopTyping(@DestinationVariable String roomId, Principal principal) {
        try {
            User user = userService.getUserEntityByEmail(principal.getName());
            messagingTemplate.convertAndSend("/room/" + roomId + "/events",
                    Map.of("type", "STOP_TYPING", "userId", user.getId(), "userName", user.getUserName(), "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            logger.error("Error handling stop typing indicator", e);
        }
    }

    /**
     * Get messages for a specific chat room.
     * This method retrieves messages for a given room, optionally filtering out messages hidden for a specific user.
     * @param roomId The ID of the chat room to retrieve messages from.
     * @param userId Optional user ID to filter out hidden messages.
     * @return List of messages in the specified chat room.
     */
    @GetMapping("/messages/{roomId}")
    public ResponseEntity<List<MessageResponseDTO>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) Long userId) {
        try {
            logger.info("Getting messages for room: {} with userId: {}", roomId, userId);
            List<MessageResponseDTO> messages;
            
            if (userId != null) {
                // Get messages excluding those hidden for this user
                messages = messageService.getRoomMessages(roomId, userId);
            } else {
                // Fallback to original method for backward compatibility
                messages = messageService.getRoomMessages(roomId);
            }
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error getting room messages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * This method allows users to soft delete multiple messages in a chat room.
     * It broadcasts a notification to the room indicating which messages were deleted.
     */
    @DeleteMapping("/messages/multiple")
    public ResponseEntity<List<Long>> deleteMultipleMessages(@RequestBody MultipleChatOperationRequest request) {
        try {
            List<Long> deletedIds = messageService.softDeleteMultipleMessages(request.messageIds(), request.userId());
            
            // Broadcast multiple delete notification to room (assuming all messages are from same room)
            if (!deletedIds.isEmpty()) {
                Message firstMessage = messageService.getMessageById(deletedIds.getFirst());
                Map<String, Object> deleteNotification = new HashMap<>();
                deleteNotification.put("type", "MULTIPLE_MESSAGES_DELETED");
                deleteNotification.put("messageIds", deletedIds);
                deleteNotification.put("softDeleted", true);
                deleteNotification.put("userId", request.userId());
                
                messagingTemplate.convertAndSend(
                    "/room/" + firstMessage.getChatroom().getChatroomId() + "/events",
                    deleteNotification
                );
            }
            
            return ResponseEntity.ok(deletedIds);
        } catch (Exception e) {
            logger.error("Error deleting multiple messages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Forward multiple messages to a target chat room.
     * This method allows users to forward multiple messages to another chat room.
     * It broadcasts each forwarded message to the target room.
     */
    @PostMapping("/messages/forward/multiple")
    public ResponseEntity<List<MessageResponseDTO>> forwardMultipleMessages(@RequestBody MultipleChatOperationRequest request) {
        try {
            List<MessageResponseDTO> forwardedMessages = messageService.forwardMultipleMessages(
                    request.messageIds(), request.targetRoomId(), request.userId());
                    
            // Broadcast each forwarded message to the target room
            for (MessageResponseDTO message : forwardedMessages) {
                messagingTemplate.convertAndSend(
                    "/room/" + request.targetRoomId() + "/messages",
                    message
                );
            }
            
            return ResponseEntity.ok(forwardedMessages);
        } catch (Exception e) {
            logger.error("Error forwarding multiple messages", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Edit a single message
     */
    @PutMapping("/messages/{messageId}/edit")
    public ResponseEntity<MessageResponseDTO> editMessage(
            @PathVariable Long messageId,
            @RequestBody MessageEditRequest request,
            @RequestParam Long userId) {
        try {
            Message editedMessage = messageService.editMessage(messageId, request.newContent(), userId);
            MessageResponseDTO response = messageService.convertToResponseDTO(editedMessage);

            // Broadcast edit notification to room
            Map<String, Object> editNotification = new HashMap<>();
            editNotification.put("type", "MESSAGE_EDITED");
            editNotification.put("messageId", messageId);
            editNotification.put("newContent", request.newContent());
            editNotification.put("edited", true);
            editNotification.put("editTimestamp", editedMessage.getEditTimestamp().toString());

            messagingTemplate.convertAndSend(
                    "/room/" + editedMessage.getChatroom().getChatroomId() + "/events",
                    editNotification
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error editing message {}: {}", messageId, e.getMessage());
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a single message (soft delete)
     * It broadcasts a notification to the room indicating the message was deleted.
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        try {
            Message message = messageService.getMessageById(messageId);
            messageService.softDeleteSingleMessage(messageId, userId);

            // Broadcast delete notification to room
            Map<String, Object> deleteNotification = new HashMap<>();
            deleteNotification.put("type", "MESSAGE_DELETED");
            deleteNotification.put("messageId", messageId);
            deleteNotification.put("softDeleted", true);

            messagingTemplate.convertAndSend(
                    "/room/" + message.getChatroom().getChatroomId() + "/events",
                    deleteNotification
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting message {}: {}", messageId, e.getMessage());
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Forward a single message
     * It broadcasts the forwarded message to the target room.
     */
    @PostMapping("/messages/{messageId}/forward")
    public ResponseEntity<MessageResponseDTO> forwardMessage(
            @PathVariable Long messageId,
            @RequestBody MessageForwardRequest request) {
        try {
            Message forwardedMessage = messageService.forwardMessage(messageId, request.targetRoomId(), request.senderId());
            MessageResponseDTO response = messageService.convertToResponseDTO(forwardedMessage);

            // Broadcast forwarded message to target room
            messagingTemplate.convertAndSend(
                    "/room/" + request.targetRoomId() + "/messages",
                    response
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error forwarding message {}: {}", messageId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * This method allows users to reply to a specific message in a chat room.
     * It creates a new message that references the original message being replied to.
     * It broadcasts the reply to the room and updates the message status.
     */
    @PostMapping("/messages/{messageId}/reply")
    public ResponseEntity<MessageResponseDTO> replyToMessage(
            @PathVariable Long messageId,
            @RequestBody MessageDTO replyMessageDTO) {
        try {
            MessageResponseDTO response = messageService.sendReplyMessage(replyMessageDTO, messageId);

            // Broadcast reply to room
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("id", response.id());
            broadcastMessage.put("content", response.content());
            broadcastMessage.put("senderId", replyMessageDTO.senderId());
            broadcastMessage.put("senderName", response.senderName());
            broadcastMessage.put("chatRoomId", response.chatRoomId());
            broadcastMessage.put("timestamp", response.timestamp().toString());
            broadcastMessage.put("messageStatus", "SENT");
            broadcastMessage.put("repliedToMessage", response.repliedToMessage());
            broadcastMessage.put("edited", response.edited());
            broadcastMessage.put("forwarded", response.forwarded());

            if (response.fileUrl() != null) {
                broadcastMessage.put("fileUrl", response.fileUrl());
                broadcastMessage.put("fileName", response.fileName());
                broadcastMessage.put("fileSize", response.fileSize());
                broadcastMessage.put("fileType", response.fileType());
                broadcastMessage.put("uploadedByUserId", response.uploadedByUserId());
                broadcastMessage.put("uploadedByUserName", response.uploadedByUserName());
            }

            messagingTemplate.convertAndSend("/room/" + response.chatRoomId() + "/messages", broadcastMessage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error sending reply to message {}: {}", messageId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}