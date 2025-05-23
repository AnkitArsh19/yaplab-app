package com.ankitarsh.securemessaging.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MessageDTO(
        @NotNull Long senderId,
        Long receiverId,
        String content,
        Long groupId,
        String fileUrl,
        String fileName,
        Long fileSize
) {
}
