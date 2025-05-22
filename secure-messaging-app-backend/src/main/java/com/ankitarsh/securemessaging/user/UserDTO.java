package com.ankitarsh.securemessaging.user;

import jakarta.validation.constraints.*;

public record UserDTO (
        @NotNull Long id,
        @NotEmpty @Size(min = 5 , max = 50) String userName,
        @NotEmpty @Email String emailId,
        @NotEmpty @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @NotEmpty @Size(min = 6) String password
){
}
