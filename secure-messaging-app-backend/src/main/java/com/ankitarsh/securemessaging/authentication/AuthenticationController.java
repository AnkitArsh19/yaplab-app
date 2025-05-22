package com.ankitarsh.securemessaging.authentication;

import com.ankitarsh.securemessaging.user.UserResponseDTO;
import com.ankitarsh.securemessaging.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user.
     * @param newUserDetails Details of the user in a body.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/register")
    public UserResponseDTO postNewUser(
            @RequestBody RegisterRequestDTO newUserDetails) {
        return this.userService.registerUser(newUserDetails);
    }

    /**
     * Login an existing user.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(
            @RequestBody LoginRequestDTO loginRequest) {
        String emailId = loginRequest.emailId();
        String loginPassword = loginRequest.password();
        LoginResponseDTO response = userService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }
}
