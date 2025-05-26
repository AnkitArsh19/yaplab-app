package com.yaplab.security.authentication;

import com.yaplab.enums.UserStatus;

public record RegisterResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status,
        String profilePictureUrl
) {
}
