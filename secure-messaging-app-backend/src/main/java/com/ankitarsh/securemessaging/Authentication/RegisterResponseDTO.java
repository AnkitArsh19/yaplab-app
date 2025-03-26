package com.ankitarsh.securemessaging.Authentication;

import com.ankitarsh.securemessaging.enums.UserStatus;

public record RegisterResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status
) {
}
