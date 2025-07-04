package com.yaplab.message;

import java.util.List;

/**
 * A DTO for operation on multiple chats
 */
public record MultipleChatOperationRequest(
        List<Long> messageIds,
        String operation,
        String targetRoomId,
        Long userId
) {}