package com.yaplab.security.authentication.passwordreset;

import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for forgot password request.
 */
public record ResetPasswordRequestDTO(
        @NotEmpty String token,
        @NotEmpty String newPassword
) {
}
