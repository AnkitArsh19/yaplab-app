package com.ankitarsh.securemessaging.authentication;

import com.ankitarsh.securemessaging.enums.UserStatus;

public record LoginResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status,
        String accessToken,
        String refreshToken
) {
}
