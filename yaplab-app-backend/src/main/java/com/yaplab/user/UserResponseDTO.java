package com.yaplab.user;

import com.yaplab.enums.UserStatus;

import java.time.Instant;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param id UserId of the user
 * @param userName UserName of the user with constraints
 * @param emailId EmailId of the user
 * @param mobileNumber MobileNumber of the user
 * @param userStatus The current status of the user
 * @param profilePictureUrl The profile picture url of the user
 * @param createdAt The time when the user registered
 * @param updatedAt The time when user last updated the personal details
 */
public record UserResponseDTO(
       Long id,
       String userName,
       String emailId,
       String mobileNumber,
       UserStatus userStatus,
       String profilePictureUrl,
       Instant createdAt,
       Instant updatedAt
) {
}
