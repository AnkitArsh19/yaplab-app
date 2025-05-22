package com.ankitarsh.securemessaging.user;

import com.ankitarsh.securemessaging.enums.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UserResponseDTO(
       @NotNull  Long id,
       @NotEmpty String userName,
       @NotEmpty String emailId,
       @NotEmpty String mobileNumber,
       UserStatus isOnline
) {
}
