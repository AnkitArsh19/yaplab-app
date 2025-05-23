package com.ankitarsh.securemessaging.files;

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