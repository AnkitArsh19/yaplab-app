package com.ankitarsh.securemessaging.Message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MessageDTO(
        @NotNull Long senderId,
        Long receiverId,
        @NotEmpty String content,
        Long groupId
) {
}
