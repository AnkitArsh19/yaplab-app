package com.ankitarsh.securemessaging.Authentication;

import jakarta.validation.constraints.NotEmpty;

public record RegisterRequestDTO(
        @NotEmpty String username,
        @NotEmpty String emailId,
        @NotEmpty String mobileNumber,
        @NotEmpty String password
) {
}
