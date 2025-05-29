package com.yaplab.security.authentication;

import com.resend.core.exception.ResendException;
import com.yaplab.security.authentication.passwordreset.ForgotPasswordRequestDTO;
import com.yaplab.security.authentication.passwordreset.PasswordChangeRequestDTO;
import com.yaplab.security.authentication.passwordreset.ResetPasswordRequestDTO;
import com.yaplab.security.token.AccessTokenResponseDTO;
import com.yaplab.security.token.RefreshTokenRequestDTO;
import com.yaplab.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for handling user authentication operations.
 * This includes user registration, login, logout, password change,
 */
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
     */
    @PostMapping("/register")
    public RegisterResponseDTO postNewUser(
            @RequestBody RegisterRequestDTO newUserDetails) throws ResendException {
        return this.userService.registerUser(newUserDetails);
    }

    /**
     * Login an existing user.
     * returns the response DTO containing user details and tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authenticationService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Login an existing user
     * returns the user registered and saved in the database.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(@RequestHeader("authorization") String authHeader) {
        authenticationService.logout(authHeader);
        return ResponseEntity.ok().build();
    }

    /**
     * Refreshes the access token using the refresh token.
     * @param requestDTO contains the refresh token.
     * @return a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDTO> refreshAccessToken(
            @RequestBody RefreshTokenRequestDTO requestDTO){
        AccessTokenResponseDTO response = authenticationService.refreshAccessToken(requestDTO.refreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Changes the password of the user.
     * @param request contains the old and new password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody PasswordChangeRequestDTO request
    ){
        authenticationService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Sends a password reset link to the user's email.
     * @param requestDTO contains the email ID of the user.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO requestDTO
    ) throws ResendException {
        authenticationService.sendPasswordResetLink(requestDTO.emailId());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the user's email using the token sent to their email.
     * @param token the verification token.
     * @return a redirect view to the login page with a success or error message.
     */
    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam("token") String token) {
        try {
            authenticationService.verifyEmail(token);
            return new RedirectView("http://localhost:5173/auth/login?verified=true");
        } catch (IllegalArgumentException e) {
            return new RedirectView("http://localhost:5173/auth/login?error=" + e.getMessage());
        }
    }

    /**
     * Resets the user's password using the token sent to their email.
     * @param requestDTO contains the token and the new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequestDTO requestDTO
    ){
        authenticationService.resetPassword(requestDTO.token(),requestDTO.newPassword());
        return ResponseEntity.ok().build();
    }
}
