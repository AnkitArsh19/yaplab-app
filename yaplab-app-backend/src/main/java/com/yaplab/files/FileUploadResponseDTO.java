package com.yaplab.files;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param id ID of the file
 * @param fileName Name of the file
 * @param fileType Type of the file(audio, video, etc.)
 * @param fileSize Size of the file(in Bytes)
 * @param fileUrl Url of the stored destination
 * @param uploadedByUserId UserId of the user who uploaded
 * @param uploadedByUserName UserName of the user who uploaded
 * @param uploadedAt Time at which the file was uploaded
 */
public record FileUploadResponseDTO(
        Long id,
        String fileName,
        String fileType,
        Long fileSize,
        String fileUrl,
        Long uploadedByUserId,
        String uploadedByUserName,
        String uploadedAt
) {}