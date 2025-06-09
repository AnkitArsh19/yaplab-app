package com.yaplab.security.authentication;

import com.yaplab.enums.UserStatus;

/**
 * Response sent to the user on a successful registration
 */
public record RegisterResponseDTO(
        Long id,
        String userName,
        String emailId,
        String mobileNumber,
        UserStatus status,
        String message
) {
}
