package com.ankitarsh.securemessaging.authentication;

import com.ankitarsh.securemessaging.enums.UserStatus;

public record RegisterResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status
) {
}
