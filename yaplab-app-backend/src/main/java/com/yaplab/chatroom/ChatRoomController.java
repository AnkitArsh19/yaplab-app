package com.yaplab.chatroom;

import com.yaplab.message.MessageResponseDTO;
import com.yaplab.message.MessageStatusService;
import com.yaplab.message.MessageService;
import com.yaplab.user.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for creating chatrooms, retrieving messages, adding and removing participants from groups.
 */
@Controller
@RequestMapping("/chatrooms")
public class ChatRoomController {
    public final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageStatusService messageStatusService;
    private final MessageService messageService;

    public ChatRoomController(ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate, MessageStatusService messageStatusService, MessageService messageService
    ){
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
        this.messageStatusService = messageStatusService;
        this.messageService = messageService;
    }

    /**
     * Finds or creates a personal chat room between two users.
     * @param chatRoomDTO DTO containing participant IDs.
     * @return ResponseEntity with ChatRoomResponseDTO
     */
    @PostMapping("/personal")
    public ResponseEntity<ChatRoomResponseDTO> getOrCreatePersonalChatroom(
            @RequestBody ChatRoomDTO chatRoomDTO
    ){
        return ResponseEntity.ok(chatRoomService.getOrCreatePersonalChatRoom(chatRoomDTO));
    }

    /**
     * Finds or creates a group chat room for a group.
     * @param chatRoomDTO DTO containing the group ID.
     * @return ResponseEntity with ChatRoomResponseDTO
     */
    @PostMapping("/group")
    public ResponseEntity<ChatRoomResponseDTO> getOrCreateGroupChatroom(
            @RequestBody ChatRoomDTO chatRoomDTO
    ){
        return ResponseEntity.ok(chatRoomService.getOrCreateGroupChatRoom(chatRoomDTO));
    }

    /**
     * Returns a list of chatroom response DTO's associated with the user
     * @param userId ID of the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatRoomResponseDTO>> getUserChatRooms(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(chatRoomService.getUserChatRooms(userId));
    }

    /**
     * Returns the list of messages of the particular chatroom
     * @param chatroomId ID of the chatroom
     * @param userId Optional user ID to filter out messages hidden for this user
     */
    @GetMapping("/{chatroomId}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesFromChatroom(
            @PathVariable String chatroomId,
            @RequestParam(required = false) Long userId
    ){
        if (userId != null) {
            // Get messages excluding those hidden for this user
            return ResponseEntity.ok(messageService.getRoomMessages(chatroomId, userId));
        } else {
            // Fallback to original method for backward compatibility
            return ResponseEntity.ok(chatRoomService.getMessagesFromChatRoom(chatroomId));
        }
    }

    /**
     * Users can connect to a chatroom by its ID
     * Uses a messaging template to send response
     * Header is used to get chatroomId from the header of a STOMP SEND frame
     * @param chatroomId ID of the chatroom
     */
    @MessageMapping("/chatroom.join/{chatroomId}")
    public void joinChatroom(
            @DestinationVariable String chatroomId,
            @Payload UserDTO user,
            @Header("simpSessionId") String sessionId) {

        // Use the centralized message status service to update message statuses
        messageStatusService.markUndeliveredMessagesAsDelivered(chatroomId, user.id());

        // Broadcast that user joined the chatroom
        messagingTemplate.convertAndSend(
                "/room/" + chatroomId + "/events",
                Map.of("type", "USER_JOINED", "user", user)
        );
    }

    /**
     * Clear chat for a specific user - hides all existing messages in the chatroom for that user
     */
    @PostMapping("/{chatroomId}/clear")
    public ResponseEntity<Map<String, String>> clearChatForUser(
            @PathVariable String chatroomId,
            @RequestParam Long userId) {
        try {
            messageService.clearChatForUser(chatroomId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Chat cleared successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to clear chat");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Delete personal chat (clear all messages for the user)
     */
    @PostMapping("/{chatroomId}/delete")
    public ResponseEntity<Map<String, String>> deletePersonalChat(
            @PathVariable String chatroomId,
            @RequestParam Long userId) {
        try {
            chatRoomService.clearMessagesForUser(chatroomId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Chat deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to delete chat");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
