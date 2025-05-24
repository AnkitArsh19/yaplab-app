package com.yaplab.authentication;

import com.yaplab.token.AccessTokenResponseDTO;
import com.yaplab.token.RefreshTokenRequestDTO;
import com.yaplab.user.UserService;
import com.resend.core.exception.ResendException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    /**
     * Constructor based dependency injection of User Service.
     */
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    /**
     * Registers a new user.
     * @param newUserDetails Details of the user in a body.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/register")
    public RegisterResponseDTO postNewUser(
            @RequestBody RegisterRequestDTO newUserDetails) throws ResendException {
        return this.userService.registerUser(newUserDetails);
    }

    /**
     * Login an existing user.
     * returns the user registered and saved in the database.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authenticationService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(@RequestHeader("authorization") String authHeader) {
        authenticationService.logout(authHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDTO> refresh(
            @RequestBody RefreshTokenRequestDTO requestDTO){
        AccessTokenResponseDTO response = authenticationService.refreshAccessToken(requestDTO.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody PasswordChangeRequestDTO request
    ){
        authenticationService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO requestDTO
    ) throws ResendException {
        authenticationService.sendPasswordResetLink(requestDTO.emailId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequestDTO requestDTO
    ){
        authenticationService.resetPassword(requestDTO.token(),requestDTO.newPassword());
        return ResponseEntity.ok().build();
    }
}
