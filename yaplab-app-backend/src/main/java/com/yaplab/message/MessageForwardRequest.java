package com.yaplab.message;

/**
 * A DTO for forwarding a message to a different chatroom.
 */
public record MessageForwardRequest(
        Long messageId,
        String targetRoomId,
        Long senderId
) {}
