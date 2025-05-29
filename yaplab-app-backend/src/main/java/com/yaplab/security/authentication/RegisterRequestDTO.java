package com.yaplab.security.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Object to request data from a user to register
 * @param userName name of the user
 * @param emailId email ID of the user
 * @param mobileNumber mobile number of the user
 * @param password password of the user
 */
public record RegisterRequestDTO(
        @NotEmpty @Size(min = 5 , max = 50) String userName,
        @NotEmpty @Email String emailId,
        @NotEmpty @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @NotEmpty @Size(min = 6) String password
) {
}
