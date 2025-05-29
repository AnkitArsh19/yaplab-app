package com.yaplab.security.token;


/**
 * Object to request a refresh token
 */
public record RefreshTokenRequestDTO(
        String refreshToken
) {
}
