package com.yaplab.security.authentication;

import com.yaplab.security.authentication.passwordreset.ForgotPasswordRequestDTO;
import com.yaplab.security.authentication.passwordreset.PasswordChangeRequestDTO;
import com.yaplab.security.authentication.passwordreset.ResetPasswordRequestDTO;
import com.yaplab.security.token.AccessTokenResponseDTO;
import com.yaplab.security.token.RefreshTokenRequestDTO;
import com.yaplab.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Controller for handling user authentication operations.
 * This includes user registration, login, logout, password change,
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

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
     * Checks the response from userService to determine the status of the registration.
     * If the user already exists and is verified, it returns a 400 Bad Request with a specific message.
     * @param registerRequestDTO Details of the user in a body.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDTO registerRequestDTO) {
        try {
            RegisterResponseDTO response = userService.registerUser(registerRequestDTO);

            if (response.message() != null) {
                if (response.message().contains("verification email has been sent") ||
                        response.message().contains("expired")) {
                    return ResponseEntity.ok(Map.of(
                            "status", "EMAIL_RESENT",
                            "message", response.message()
                    ));
                } else if (response.message().contains("check your email for the verification link")) {
                    return ResponseEntity.ok(Map.of(
                            "status", "EMAIL_PENDING",
                            "message", response.message()
                    ));
                }
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists and is verified")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", "USER_EXISTS",
                                "message", e.getMessage()
                        ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration failed for {}: {}", registerRequestDTO.emailId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed due to server error."));
        }
    }

    /**
     * Login an existing user.
     * If the email is not verified, it sends a verification email.
     * If the user is not found or credentials are invalid, it returns appropriate error messages.
     * @param loginRequest contains the email and password of the user.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        try {
            LoginResponseDTO response = authenticationService.loginUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Email not verified")) {
                try {
                    userService.resendVerificationEmail(loginRequest.emailId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "status", "EMAIL_NOT_VERIFIED_RESENT",
                                    "message", "Email not verified. A verification email has been sent to your inbox.",
                                    "showResend", false
                            ));
                } catch (Exception emailException) {
                    logger.error("Failed to send verification email during login for {}: {}",
                            loginRequest.emailId(), emailException.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "status", "EMAIL_NOT_VERIFIED",
                                    "message", "Email not verified. Please check your inbox or use the resend option.",
                                    "showResend", true
                            ));
                }
            } else if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "User not found with the provided email."));
            } else if (e.getMessage().contains("Invalid Credentials")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid email or password."));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Login failed for {}: {}", loginRequest.emailId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Login failed due to server error."));
        }
    }

    /**
     * Logs out the user.
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
    ) {
        authenticationService.sendPasswordResetLink(requestDTO.emailId());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the user's email using the token sent to their email.
     * After successful verification, redirects to the frontend login page.
     * @param token the verification token.
     * @return a redirect to the login page or error response.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            authenticationService.verifyEmail(token);

            response.sendRedirect("http://localhost:5173/");
            return null;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("expired")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", "TOKEN_EXPIRED",
                                "message", "The verification link has expired. Please request a new one."
                        ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify email."));
        }
    }

    /**
     * Resends verification email for unverified users.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String emailId) {
        try {
            userService.resendVerificationEmail(emailId);
            return ResponseEntity.ok(Map.of("message", "Verification email sent successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to resend verification email to {}: {}", emailId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send verification email"));
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
