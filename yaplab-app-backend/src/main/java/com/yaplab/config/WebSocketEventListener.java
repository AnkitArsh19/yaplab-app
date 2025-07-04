package com.yaplab.config;

import com.yaplab.chatroom.ChatRoomService;
import com.yaplab.message.MessageStatusService;
import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

/**
 * WebSocketEventListener listens for WebSocket connection and disconnection events.
 * It manages user sessions, delivers pending messages, and updates user status.
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserService userService;
    private final WebSocketConnectionManager connectionManager;
    private final MessageStatusService messageStatusService;
    private final ChatRoomService chatRoomService;

    public WebSocketEventListener(UserService userService, WebSocketConnectionManager connectionManager,
                                  MessageStatusService messageStatusService, ChatRoomService chatRoomService) {
        this.userService = userService;
        this.connectionManager = connectionManager;
        this.messageStatusService = messageStatusService;
        this.chatRoomService = chatRoomService;
    }

    /**
     * Handles WebSocket connection events.
     * When a user connects, it adds the user session and delivers any pending messages.
     * @param event the SessionConnectedEvent containing connection details
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (headerAccessor.getUser() != null) {
            String userEmail = headerAccessor.getUser().getName();
            try {
                Long userId = userService.getUserEntityByEmail(userEmail).getId();
                connectionManager.addUserSession(userId, sessionId);
                userService.connect(userId);
                deliverPendingMessages(userId);
            } catch (Exception e) {
                logger.error("Error handling WebSocket connect: {}", e.getMessage());
            }
        }
    }

    /**
     * Handles WebSocket disconnection events.
     * When a user disconnects, it removes the user session and updates their online status.
     * @param event the SessionDisconnectEvent containing disconnection details
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            try {
                Long userId = connectionManager.getUserBySession(sessionId);
                if (userId != null) {
                    connectionManager.removeUserSession(userId, sessionId);
                    if (!connectionManager.isUserOnline(userId)) {
                        userService.disconnect(userId);
                    }
                }
            } catch (Exception e) {
                logger.error("Error handling WebSocket disconnect: {}", e.getMessage());
            }
        }
    }

    /**
     * Delivers any pending messages for the user when they connect.
     * It marks undelivered messages as delivered in all chatrooms the user is part of.
     * @param userId the ID of the user to deliver messages for
     */
    private void deliverPendingMessages(Long userId) {
        try {
            List<String> userChatrooms = chatRoomService.getUserChatroomIds(userId);

            for (String chatroomId : userChatrooms) {
                messageStatusService.markUndeliveredMessagesAsDelivered(chatroomId, userId);
            }
        } catch (Exception e) {
            logger.error("Error delivering pending messages for user {}: {}", userId, e.getMessage());
        }
    }
}