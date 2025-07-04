package com.yaplab.config;

import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * WebSocketConnectionManager manages WebSocket connections and user sessions.
 * It tracks active sessions, handles heartbeats, and cleans up stale connections.
 * This service is used to maintain real-time communication in the YapLab application.
 */
@Service
public class WebSocketConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    /**
     * Concurrent maps for a multithreaded environment to track user sessions and their heartbeats.
     * userSessions maps user IDs to their active WebSocket session IDs.
     */
    private final ConcurrentMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * sessionUsers maps WebSocket session IDs to user IDs.
     * sessionHeartbeats tracks the last heartbeat time for each session.
     */
    private final ConcurrentMap<String, Long> sessionUsers = new ConcurrentHashMap<>();

    /**
     * sessionHeartbeats tracks the last heartbeat time for each session.
     * This is used to determine if a session is stale based on the heartbeat timeout.
     */
    private final ConcurrentMap<String, Instant> sessionHeartbeats = new ConcurrentHashMap<>();

    /**
     * UserService is injected lazily to avoid circular dependency issues.
     * It is used to manage user connections and disconnections.
     */
    private final UserService userService;

    /**
     * Timeout for stale connections in seconds.
     * This is the threshold after which a connection is considered stale and eligible for cleanup.
     */
    private static final int STALE_CONNECTION_TIMEOUT_SECONDS = 60;

    /**
     * Rate at which to sweep stale connections, in milliseconds.
     * This determines how often the system checks for and cleans up stale WebSocket connections.
     */
    private static final long SWEEP_STALE_CONNECTIONS_RATE_MS = 30000;

    /**
     * Constructor for WebSocketConnectionManager.
     * Uses @Lazy to avoid circular dependency issues with UserService.
     * @param userService the UserService to manage user connections and disconnections
     */
    public WebSocketConnectionManager(
            @Lazy
            UserService userService
    ) {
        this.userService = userService;
    }

    /**
     * Checks if a user is online based on their user ID.
     * A user is considered online if they have at least one active session.
     */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    /**
     * Retrieves all active session IDs for a given user ID.
     * If the user has no active sessions, an empty set is returned.
     */
    public Set<String> getUserSessions(Long userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    /**
     * Retrieves the user ID associated with a given WebSocket session ID.
     * If the session ID is not found, it returns null.
     */
    public Long getUserBySession(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    /**
     * Adds a new user session to the tracking maps.
     * This method is called when a user connects via WebSocket.
     * It updates the userSessions, sessionUsers, and sessionHeartbeats maps.
     */
    public void addUserSession(Long userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        sessionUsers.put(sessionId, userId);
        sessionHeartbeats.put(sessionId, Instant.now());
    }

    /**
     * Removes a user session from the tracking maps.
     * This method is called when a user disconnects or when a session is no longer active.
     * It updates the userSessions, sessionUsers, and sessionHeartbeats maps accordingly.
     */
    public void removeUserSession(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        sessionUsers.remove(sessionId);
        sessionHeartbeats.remove(sessionId);
    }

    /**
     * Removes a session from all tracking maps, used when the user ID might be unknown
     * or when cleaning up a session that might not have been fully associated.
     * @param sessionId The session ID to remove.
     */
    private void removeSessionOnly(String sessionId) {
        Long userId = sessionUsers.remove(sessionId);
        sessionHeartbeats.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        if (userId == null) {
            userSessions.forEach((uid, sessionSet) -> {
                if (sessionSet.remove(sessionId)) {
                    if (sessionSet.isEmpty()) {
                        userSessions.remove(uid);
                    }
                }
            });
        }
    }

    /**
     * Updates the heartbeat for a given session ID.
     * This method is called periodically to refresh the last active time of a session.
     * If the session ID is not found, it logs a warning and removes the session from tracking.
     */
    public void updateHeartbeat(String sessionId) {
        if (sessionUsers.containsKey(sessionId)) {
            sessionHeartbeats.put(sessionId, Instant.now());
        } else {
            logger.warn("[WS-CONN] Attempted to update heartbeat for unknown or removed session: {}", sessionId);
            sessionHeartbeats.remove(sessionId);
        }
    }

    /**
     * Retrieves a set of stale connections based on the specified timeout in seconds.
     * A connection is considered stale if its last heartbeat was before the threshold time.
     * @param timeoutSeconds The timeout in seconds to determine if a connection is stale.
     * @return A set of session IDs that are considered stale.
     */
    public Set<String> getStaleConnections(int timeoutSeconds) {
        Instant threshold = Instant.now().minusSeconds(timeoutSeconds);
        return sessionHeartbeats.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(threshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Scheduled task that runs periodically to sweep stale WebSocket connections.
     * It checks for connections that have not sent a heartbeat within the specified timeout
     * and removes them from the tracking maps.
     */
    @Scheduled(fixedRate = SWEEP_STALE_CONNECTIONS_RATE_MS)
    public void sweepStaleConnections() {
        Set<String> staleSessionIds = getStaleConnections(STALE_CONNECTION_TIMEOUT_SECONDS);
        if (!staleSessionIds.isEmpty()) {
            logger.warn("Found {} stale WebSocket session(s) to clean up: {}", staleSessionIds.size(), staleSessionIds);
        } else {
            return;
        }
        for (String sessionId : staleSessionIds) {
            Long userId = getUserBySession(sessionId);
            if (userId != null) {
                logger.warn("Cleaning up stale WebSocket session: {} for user: {}", sessionId, userId);
                removeUserSession(userId, sessionId);
                if (!isUserOnline(userId)) {
                    try {
                        userService.disconnect(userId);
                    } catch (Exception e) {
                        logger.error("Error marking user {} as disconnected for stale session {}: {}", userId, sessionId, e.getMessage(), e);
                    }
                }
            } else {
                logger.warn("Cleaning up stale session {} which had no associated user ID in sessionUsers map. Forcing removal.", sessionId);
                removeSessionOnly(sessionId);
            }
        }
    }
}