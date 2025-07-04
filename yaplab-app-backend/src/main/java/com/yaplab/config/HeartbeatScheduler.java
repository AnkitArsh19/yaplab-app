package com.yaplab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * HeartbeatScheduler is a service that periodically checks for stale WebSocket connections
 * and cleans them up if they are older than a specified threshold.
 * It uses the WebSocketConnectionManager to manage user sessions and connections.
 */
@Service
public class HeartbeatScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final WebSocketConnectionManager connectionManager;

    public HeartbeatScheduler(WebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Scheduled task that runs every minute to check for stale WebSocket connections.
     * If a connection is older than 90 seconds, it will be removed from the session.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupStaleConnections() {
        try {
            // Get stale connections that are older than 90 seconds
            Set<String> staleConnections = connectionManager.getStaleConnections(90);

            if (!staleConnections.isEmpty()) {
                for (String sessionId : staleConnections) {
                    Long userId = connectionManager.getUserBySession(sessionId);
                    if (userId != null) {
                        connectionManager.removeUserSession(userId, sessionId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during stale connection cleanup: {}", e.getMessage(), e);
        }
    }
}