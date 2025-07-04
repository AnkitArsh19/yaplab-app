package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing message status updates in chatrooms
 */
@Service
public class MessageStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MessageStatusService.class);

    /**
     * Constructor based injection for dependencies.
     */
    private final MessageRepository messageRepository;
    private final UserMessageStatusRepository userMessageStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public MessageStatusService(
            MessageRepository messageRepository,
            UserMessageStatusRepository userMessageStatusRepository,
            SimpMessagingTemplate messagingTemplate,
            UserService userService) {
        this.messageRepository = messageRepository;
        this.userMessageStatusRepository = userMessageStatusRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    /**
     * Updates the status of messages in a chatroom.
     * For group messages, tracks per-user status and only updates overall message status
     * when all participants have reached that status.
     * @param chatroomId  The ID of the chatroom.
     * @param userId      The ID of the user performing the update.
     * @param messageIds  List of message IDs to update.
     * @param newStatus   The new status to set for the messages.
     */
    @Transactional
    public void updateMessageStatus(String chatroomId, Long userId, List<Long> messageIds, MessageStatus newStatus) {
        List<Message> messages = messageRepository.findAllById(messageIds);
        List<Long> updatedIds = new ArrayList<>();
        User currentUser = userService.getUserEntityByID(userId);

        for (Message message : messages) {
            if (message.getChatroom().getChatroomId().equals(chatroomId)) {
                boolean canUpdateStatus = canUpdateStatus(newStatus, message, currentUser);

                if (canUpdateStatus && isValidStatusTransition(message.getMessageStatus(), newStatus)) {
                    // Handle group messages differently
                    if (isGroupMessage(message)) {
                        handleGroupMessageStatusUpdate(message, currentUser, newStatus, updatedIds);
                    } else {
                        // For personal messages, update directly
                        message.setMessageStatus(newStatus);
                        messageRepository.save(message);
                        updatedIds.add(message.getId());
                    }
                }
            }
        }

        // If any messages were updated, broadcast the status change
        if (!updatedIds.isEmpty()) {
            broadcastStatusUpdate(chatroomId, updatedIds, newStatus);
        }
    }

    /**
     * Handles status updates for group messages by tracking per-user status.
     */
    private void handleGroupMessageStatusUpdate(Message message, User user, MessageStatus newStatus, List<Long> updatedIds) {
        UserMessageStatus userStatus = userMessageStatusRepository
                .findByMessageIdAndUserId(message.getId(), user.getId())
                .orElse(new UserMessageStatus(message, user, MessageStatus.SENT));
        
        // Only update if it's a valid transition
        if (isValidStatusTransition(userStatus.getStatus(), newStatus)) {
            userStatus.setStatus(newStatus);
            userMessageStatusRepository.save(userStatus);

            // Check if all participants have reached this status
            if (shouldUpdateOverallMessageStatus(message, newStatus)) {
                message.setMessageStatus(newStatus);
                messageRepository.save(message);
                updatedIds.add(message.getId());
            }
        }
    }

    /**
     * Determines if the overall message status should be updated based on all participants' status.
     */
    private boolean shouldUpdateOverallMessageStatus(Message message, MessageStatus targetStatus) {
        // Get all participants except the sender
        Set<User> participants = message.getChatroom().getParticipants();
        int participantCount = participants.size() - 1; // Exclude sender
        
        if (participantCount <= 0) {
            return true; // No other participants, can update
        }

        // Count how many participants have reached the target status
        long usersWithTargetStatus = userMessageStatusRepository
                .countByMessageIdAndStatus(message.getId(), targetStatus);

        // Only update overall status if ALL participants (except sender) have reached target status
        return usersWithTargetStatus >= participantCount;
    }

    /**
     * Determines if a message is a group message.
     */
    private boolean isGroupMessage(Message message) {
        return message.getReceiver() == null && message.getGroup() != null;
    }

    /**
     * Creates initial per-user status entries for a new group message.
     */
    @Transactional
    public void initializeGroupMessageStatuses(Message message) {
        if (isGroupMessage(message)) {
            Set<User> participants = message.getChatroom().getParticipants();
            
            for (User participant : participants) {
                // Don't create status for the sender
                if (!participant.getId().equals(message.getSender().getId())) {
                    UserMessageStatus userStatus = new UserMessageStatus(message, participant, MessageStatus.SENT);
                    userMessageStatusRepository.save(userStatus);
                }
            }
        }
    }

    /**
     * Checks if the current user can update the status of a message.
     */
    private boolean canUpdateStatus(MessageStatus newStatus, Message message, User currentUser) {
        if (newStatus == MessageStatus.DELIVERED || newStatus == MessageStatus.READ) {
            // If the message is sent to a specific receiver
            if (message.getReceiver() != null && message.getReceiver().equals(currentUser)) {
                return true;
            }
            // Else the participants of the group is allowed to update the status
            else return message.getReceiver() == null && message.getChatroom().getParticipants().contains(currentUser);
        }
        // If the status is SENT only the sender can update it.
        else if (newStatus == MessageStatus.SENT) {
            return message.getSender().equals(currentUser);
        }
        return false;
    }

    /**
     * Marks all sent messages in a chatroom as delivered for a specific user.
     * For group messages, this updates the per-user status and overall message status if appropriate.
     * This is typically called when a user reconnects to ensure they receive
     * the correct status for messages they missed while offline.
     * @param chatroomId The ID of the chatroom.
     * @param userId     The ID of the user whose messages are being marked as delivered.
     */
    @Transactional
    public void markUndeliveredMessagesAsDelivered(String chatroomId, Long userId) {
        User user = userService.getUserEntityByID(userId);

        List<Message> allSentMessages = messageRepository.findByChatroom_ChatroomIdAndMessageStatus(
                chatroomId, MessageStatus.SENT);

        List<Long> updatedIds = new ArrayList<>();
        for (Message message : allSentMessages) {
            // Check if the message is sent to a specific receiver or a group chat
            if (message.getReceiver() != null && message.getReceiver().equals(user)
                    && !message.getSender().getId().equals(userId)) {
                // Personal message - update directly
                message.setMessageStatus(MessageStatus.DELIVERED);
                messageRepository.save(message);
                updatedIds.add(message.getId());
            }
            // If the message is a group message and the user is a participant
            else if (isGroupMessage(message) && message.getChatroom().getParticipants().contains(user)
                    && !message.getSender().getId().equals(userId)) {
                // Group message - update per-user status
                handleGroupMessageStatusUpdate(message, user, MessageStatus.DELIVERED, updatedIds);
            }
        }

        // If any messages were updated, broadcast the status change
        if (!updatedIds.isEmpty()) {
            broadcastStatusUpdate(chatroomId, updatedIds, MessageStatus.DELIVERED);
        }
    }

    /**
     * Validates if the transition from current status to new status is allowed.
     */
    private boolean isValidStatusTransition(MessageStatus currentStatus, MessageStatus newStatus) {
        if (currentStatus == newStatus)
            return true;

        return switch (currentStatus) {
            case NOT_SENT -> newStatus == MessageStatus.SENT;
            case SENT -> newStatus == MessageStatus.DELIVERED;
            case DELIVERED -> newStatus == MessageStatus.READ;
            default -> false;
        };
    }

    /**
     * Broadcasts a status update to all participants in the chatroom.
     * This method sends a message to the WebSocket endpoint for the chatroom to notify all users about the updated message status.
     */
    private void broadcastStatusUpdate(String chatroomId, List<Long> messageIds, MessageStatus status) {
        try {
            Map<String, Object> broadcastData = Map.of(
                    "type", "MESSAGE_STATUS_UPDATE",
                    "chatroomId", chatroomId,
                    "status", status.toString(),
                    "messageIds", messageIds,
                    "timestamp", Instant.now().toString()
            );
            messagingTemplate.convertAndSend("/room/" + chatroomId + "/events", broadcastData);

        } catch (Exception e) {
            logger.error("Failed to broadcast status update for room {}: {}", chatroomId, e.getMessage());
        }
    }

    /**
     * Gets detailed status information for a group message.
     * Returns a map of user IDs to their message status.
     */
    public Map<Long, MessageStatus> getGroupMessageStatusDetails(Long messageId) {
        List<UserMessageStatus> statuses = userMessageStatusRepository.findByMessageId(messageId);
        return statuses.stream()
                .collect(java.util.stream.Collectors.toMap(
                    status -> status.getUser().getId(),
                    UserMessageStatus::getStatus
                ));
    }

    /**
     * Gets the count of users who have achieved each status for a group message.
     */
    public Map<MessageStatus, Long> getGroupMessageStatusCounts(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!isGroupMessage(message)) {
            throw new IllegalArgumentException("Message is not a group message");
        }

        Map<MessageStatus, Long> statusCounts = new java.util.HashMap<>();
        statusCounts.put(MessageStatus.SENT, userMessageStatusRepository.countByMessageIdAndStatus(messageId, MessageStatus.SENT));
        statusCounts.put(MessageStatus.DELIVERED, userMessageStatusRepository.countByMessageIdAndStatus(messageId, MessageStatus.DELIVERED));
        statusCounts.put(MessageStatus.READ, userMessageStatusRepository.countByMessageIdAndStatus(messageId, MessageStatus.READ));
        
        return statusCounts;
    }
}