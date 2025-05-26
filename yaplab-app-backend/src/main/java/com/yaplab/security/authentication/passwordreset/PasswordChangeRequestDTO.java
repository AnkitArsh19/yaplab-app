package com.yaplab.security.authentication.passwordreset;

import jakarta.validation.constraints.NotEmpty;

public record PasswordChangeRequestDTO(
        @NotEmpty String emailId,
        @NotEmpty String oldPassword,
        @NotEmpty String newPassword
) {
}
