package com.ankitarsh.securemessaging.Authentication;

import jakarta.validation.constraints.NotEmpty;

public record LoginRequestDTO(
        @NotEmpty String emailId,
        @NotEmpty String password
) {
}
