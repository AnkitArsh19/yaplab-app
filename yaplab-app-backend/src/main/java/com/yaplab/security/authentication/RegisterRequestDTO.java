package com.yaplab.security.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotEmpty @Size(min = 5 , max = 50) String userName,
        @NotEmpty @Email String emailId,
        @NotEmpty @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @NotEmpty @Size(min = 6) String password
) {
}
