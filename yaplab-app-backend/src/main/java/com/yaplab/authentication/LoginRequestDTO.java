package com.yaplab.authentication;

import jakarta.validation.constraints.NotEmpty;

public record LoginRequestDTO(
        @NotEmpty String emailId,
        @NotEmpty String password
) {
}
