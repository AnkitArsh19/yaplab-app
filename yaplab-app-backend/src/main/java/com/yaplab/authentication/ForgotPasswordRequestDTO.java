package com.yaplab.authentication;

import jakarta.validation.constraints.NotEmpty;

public record ForgotPasswordRequestDTO(
        @NotEmpty String emailId
) {
}
