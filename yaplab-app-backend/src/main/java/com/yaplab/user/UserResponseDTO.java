package com.yaplab.user;

import com.yaplab.enums.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * Fields are marked as not empty to check for null and emptiness.
 * @param id UserId of the user
 * @param userName UserName of the user with constraints
 * @param emailId EmailId of the user
 * @param mobileNumber MobileNumber of the user.
 * @param userStatus The current status of the user.
 */
public record UserResponseDTO(
       @NotNull  Long id,
       @NotEmpty String userName,
       @NotEmpty String emailId,
       @NotEmpty String mobileNumber,
       UserStatus userStatus,
       String profilePictureUrl
) {
}
