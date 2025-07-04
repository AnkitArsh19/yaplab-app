package com.yaplab.message;

/**
 * A DTO for editing a message in a chatroom.
 * Contains the message ID and the new content for the message.
 */
public record MessageEditRequest(
        Long messageId,
        String newContent
) {}