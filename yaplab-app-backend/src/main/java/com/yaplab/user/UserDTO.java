package com.yaplab.user;

import jakarta.validation.constraints.*;

/**
 * A Data Transfer Object to transfer data between different layers.
 * Contains required information to not expose the whole Entity.
 * Fields are marked as not empty to check for null and emptiness.
 * @param id UserId of the user
 * @param userName UserName of the user with constraints
 * @param emailId EmailId of the user
 * @param mobileNumber MobileNumber of the user. Needs to have a digit from 0-9 and should repeat 10 times.
 * @param password Password of the user with minimum length of 6
 */
public record UserDTO (
        @NotNull Long id,
        @NotEmpty @Size(min = 5 , max = 50) String userName,
        @NotEmpty @Email String emailId,
        @NotEmpty @Pattern(regexp = "^[0-9]{10}$") String mobileNumber,
        @NotEmpty @Size(min = 6) String password
){
}
