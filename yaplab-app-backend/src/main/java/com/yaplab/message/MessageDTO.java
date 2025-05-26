package com.yaplab.message;

import jakarta.validation.constraints.NotNull;

/**
 * A Data Transfer Object (DTO) for sending messages.
 * This DTO is used to encapsulate the data required to send a message,
 * including sender and receiver information, content, group ID, and file details.
 * @param senderId   ID of the user sending the message (required).
 * @param receiverId ID of the user receiving the message (optional).
 * @param content    Content of the message (optional).
 * @param groupId    ID of the group to which the message is sent (optional).
 * @param fileUrl    URL of the file attached to the message (optional).
 * @param fileName   Name of the file attached to the message (optional).
 * @param fileSize   Size of the file attached to the message in bytes (optional).
 * @param repliedToMessageId ID of the message replied to (optional).
 */
public record MessageDTO(
        @NotNull Long senderId,
        Long receiverId,
        String content,
        Long groupId,
        String fileUrl,
        String fileName,
        Long fileSize,
        Long repliedToMessageId
) {
}
