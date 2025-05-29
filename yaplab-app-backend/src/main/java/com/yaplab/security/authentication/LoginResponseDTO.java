package com.yaplab.security.authentication;

import com.yaplab.enums.UserStatus;

/**
 * Response sent to the user on a successful login
 * @param id ID of the user
 * @param userName name of the user
 * @param emailId emailID of the user
 * @param mobileNumber mobile number of the user
 * @param status current status of the user(offline/online)
 * @param accessToken access token for validating requests
 * @param refreshToken refresh token for validating requests
 * @param profilePictureUrl profile of the user if exists
 */
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
