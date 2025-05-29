package com.yaplab.security.authentication;

import jakarta.validation.constraints.NotEmpty;

/**
 * Object to request data from the user to login
 * @param emailId Email ID of the user
 * @param password Password of the user
 */
public record LoginRequestDTO(
        @NotEmpty String emailId,
        @NotEmpty String password
) {
}
