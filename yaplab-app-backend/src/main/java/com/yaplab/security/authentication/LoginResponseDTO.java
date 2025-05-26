package com.yaplab.security.authentication;

import com.yaplab.enums.UserStatus;

public record LoginResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status,
        String accessToken,
        String refreshToken,
        String profilePictureUrl
) {
}
