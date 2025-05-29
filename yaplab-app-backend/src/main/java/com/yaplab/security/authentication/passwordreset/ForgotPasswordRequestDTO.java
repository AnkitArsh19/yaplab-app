package com.yaplab.security.authentication.passwordreset;

import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for forgot password request.
 */
public record ForgotPasswordRequestDTO(
        @NotEmpty String emailId
) {
}
