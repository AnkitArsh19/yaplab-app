package com.yaplab.chatroom;

import com.yaplab.message.MessageResponseDTO;
import com.yaplab.user.User;
import com.yaplab.user.UserDTO;
import com.yaplab.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST Controller for handling user operations.
 * Provides endpoints for creating chatrooms, retrieving messages, adding and removing participants from groups.
 */
@Controller
@RequestMapping("/chatrooms")
public class ChatRoomController {
    public final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public ChatRoomController(ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
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
     */
    @GetMapping("/{chatroomId}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesFromChatroom(
            @PathVariable String chatroomId
    ){
        return ResponseEntity.ok(chatRoomService.getMessagesFromChatRoom(chatroomId));
    }

    /**
     * Users can connect to a chatroom by its ID
     * Uses a messaging template to send response
     * Header is used to get chatroomId from the header of a STOMP SEND frame
     * @param chatroomId ID of the chatroom
     */
    @MessageMapping("/chatroom.join")
    public void joinChatroom(@Payload UserDTO user, @Header("chatroomId") String chatroomId) {
        chatRoomService.updateLastActivity(chatroomId);
        messagingTemplate.convertAndSend("/topic/" + chatroomId, user);
    }

    /**
     * User leaves the chatroom and last activity is updated.
     * @param chatroomId ID of the chatroom
     * @param user userDTO of the user
     */
    @MessageMapping("/chatroom.leave")
    @SendTo("/topic/{chatroomId}")
    public UserDTO leaveChatroom(
            @DestinationVariable String chatroomId,
            @Payload UserDTO user
    ){
        chatRoomService.updateLastActivity(chatroomId);
        return user;
    }

    /**
     * Handles incoming "typing" events from users.
     * Broadcasts a typing indicator to other users in the chat room.
     * @param chatroomId The ID of the chat room.
     * @param principal The authenticated user principal.
     */
    @MessageMapping("/chat.typing/{chatroomId}")
    public void handleTypingEvent(
            @DestinationVariable String chatroomId,
            Principal principal
    ) {
        User user = userService.getUserEntityByEmail(principal.getName());
        TypingIndicatorMessage typingIndicator = new TypingIndicatorMessage(user.getId(), user.getUserName(), chatroomId, true);
        messagingTemplate.convertAndSend("/topic/chat/" + chatroomId + "/typing", typingIndicator);
    }

    /**
     * Handles incoming "stop typing" events from users.
     * Broadcasts a stop typing indicator to other users in the chat room.
     * @param chatroomId The ID of the chat room.
     * @param principal The authenticated user principal.
     */
    @MessageMapping("/chat.stoptyping/{chatroomId}")
    public void handleStopTypingEvent(
            @DestinationVariable String chatroomId,
            Principal principal
    ) {
        User user = userService.getUserEntityByEmail(principal.getName());
        TypingIndicatorMessage typingIndicator = new TypingIndicatorMessage(user.getId(), user.getUserName(), chatroomId, false);
        messagingTemplate.convertAndSend("/topic/chat/" + chatroomId + "/typing", typingIndicator);
    }
}
