package com.yaplab.authentication;

import jakarta.validation.constraints.NotEmpty;

public record ResetPasswordRequestDTO(
        @NotEmpty String token,
        @NotEmpty String newPassword
) {
}
