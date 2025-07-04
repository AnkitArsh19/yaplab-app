package com.yaplab.message;

import java.util.List;

/**
 * A DTO for updating the status of multiple messages in a chatroom.
 */
public record MessageStatusUpdateRequest(
        List<Long> messageIds,
        String chatroomId,
        Long userId
) {}
