package com.yaplab.user;

import com.yaplab.enums.UserStatus;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param id UserId of the user
 * @param userName UserName of the user with constraints
 * @param emailId EmailId of the user
 * @param mobileNumber MobileNumber of the user
 * @param userStatus The current status of the user
 * @param profilePictureUrl The profile picture url of the user
 */
public record UserResponseDTO(
       Long id,
       String userName,
       String emailId,
       String mobileNumber,
       UserStatus userStatus,
       String profilePictureUrl
) {
}
