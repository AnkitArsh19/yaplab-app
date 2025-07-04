package com.yaplab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * WebSocketAuthChannelInterceptor is a Spring component that intercepts WebSocket messages
 * to handle authentication for WebSocket connections.
 * It checks for a JWT token in the "Authorization" header and authenticates the user.
 * If authentication fails, it logs an error and returns null, preventing the message from being processed.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);
    private final WebSocketAuthenticatorService webSocketAuthenticatorService;
    private final WebSocketConnectionManager webSocketConnectionManager;

    public WebSocketAuthChannelInterceptor(WebSocketAuthenticatorService webSocketAuthenticatorService,
                                           WebSocketConnectionManager webSocketConnectionManager) {
        this.webSocketAuthenticatorService = webSocketAuthenticatorService;
        this.webSocketConnectionManager = webSocketConnectionManager;
    }

    /**
     * Intercepts WebSocket messages to authenticate users based on JWT tokens.
     * If the message is a CONNECT command, it checks for the "Authorization" header,
     * extracts the JWT token, and authenticates the user.
     * If authentication fails, it logs an error and returns null.
     * If successful, it updates the user's session ID in the connection manager.
     *
     * @param message the incoming WebSocket message
     * @param channel the channel to which the message is sent
     * @return the original message if authentication is successful, null otherwise
     * @throws AuthenticationException if authentication fails
     */
    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) throws AuthenticationException {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT == accessor.getCommand()) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);

                try {
                    final UsernamePasswordAuthenticationToken user =
                            this.webSocketAuthenticatorService.getAuthenticatedOrFail(jwt);
                    accessor.setUser(user);

                } catch (AuthenticationException e) {
                    logger.error("WebSocket authentication failed: {}", e.getMessage());
                    return null;
                }
            } else {
                logger.warn("No Authorization header in WebSocket CONNECT");
                return null;
            }
        }
        if (accessor != null && accessor.getSessionId() != null) {
        webSocketConnectionManager.updateHeartbeat(accessor.getSessionId());
        }

        return message;
    }
}