package com.yaplab.security.authentication.passwordreset;

import jakarta.validation.constraints.NotEmpty;

public record ResetPasswordRequestDTO(
        @NotEmpty String token,
        @NotEmpty String newPassword
) {
}
