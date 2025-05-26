package com.yaplab.security.authentication.passwordreset;

import jakarta.validation.constraints.NotEmpty;

public record ForgotPasswordRequestDTO(
        @NotEmpty String emailId
) {
}
