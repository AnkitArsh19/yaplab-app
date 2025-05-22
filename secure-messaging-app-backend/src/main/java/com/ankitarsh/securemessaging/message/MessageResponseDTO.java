package com.ankitarsh.securemessaging.message;

import com.ankitarsh.securemessaging.enums.MessageStatus;
import java.time.LocalDateTime;

public record MessageResponseDTO (
        Long id,
        String senderName,
        String content,
        LocalDateTime timestamp,
        MessageStatus  messageStatus
){
}
