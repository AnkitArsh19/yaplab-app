package com.yaplab.message;

import com.yaplab.enums.MessageStatus;

import java.time.Instant;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * Fields are marked as not empty to check for null and emptiness.
 * @param id id if the message
 * @param senderName sender of the message
 * @param content content of the message
 * @param timestamp timestamp of the message
 * @param messageStatus status of the message
 * @param fileUrl url of the file if any
 * @param fileName name of the file if any
 * @param fileSize size of the file if any
 * @param uploadedByUserId id of the user who uploaded the file if any
 * @param uploadedByUserName name of the user who uploaded the file if any
 * @param fileType type of the file if any
 * @param repliedToMessage repliedToMessage DTO being replied to
 */
public record MessageResponseDTO (
        Long id,
        String senderName,
        String content,
        Instant timestamp,
        MessageStatus  messageStatus,
        String fileUrl,
        String fileName,
        Long fileSize,
        Long uploadedByUserId,
        String uploadedByUserName,
        String fileType,
        RepliedToMessageDTO repliedToMessage
){

    public record RepliedToMessageDTO(
            Long id,
            String senderName,
            String content
    ) {}
}
