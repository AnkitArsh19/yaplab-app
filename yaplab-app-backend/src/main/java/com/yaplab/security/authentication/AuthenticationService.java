package com.yaplab.security.authentication;

import com.yaplab.enums.UserStatus;
import com.yaplab.security.JWTService;
import com.yaplab.security.authentication.passwordreset.PasswordChangeRequestDTO;
import com.yaplab.security.authentication.passwordreset.PasswordResetToken;
import com.yaplab.security.authentication.passwordreset.PasswordResetTokenRepository;
import com.yaplab.security.token.AccessTokenResponseDTO;
import com.yaplab.security.token.RefreshToken;
import com.yaplab.security.token.RefreshTokenRepository;
import com.yaplab.user.User;
import com.yaplab.user.UserMapper;
import com.yaplab.user.UserRepository;
import com.yaplab.user.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for handling user authentication, including login, logout, password reset, and email verification.
 */
@Service
public class AuthenticationService {

    /**
     * Logger for AuthenticationService
     * This logger is used to log various events and errors in the AuthenticationService class.
     * It helps in debugging and tracking the flow of operations related to authentication management.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /**
     * Constructor based dependency injection
     */
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserService userService;

    public AuthenticationService(JWTService jwtService, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, AuthenticationManager authManager, UserMapper userMapper, PasswordEncoder passwordEncoder, PasswordResetTokenRepository resetTokenRepository, EmailService emailService, EmailVerificationTokenRepository emailVerificationTokenRepository, UserService userService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authManager = authManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenRepository = resetTokenRepository;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userService = userService;
    }

    /**
     * Login a user to the existing account.
     * The user is found in the database using emailId.
     * Authentication object is created by authManager by checking the password and username.
     * The status is updated to online and token is generated to send in the response.
     * Marked as transactional to keep the method atomic due to multiple database operations
     * At every login, a new refresh token is generated and old ones are revoked.
     * @param loginRequestDTO The object containing username and password
     * @return The login response DTO
     */
    @Transactional
    public LoginResponseDTO loginUser(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmailId(loginRequestDTO.emailId())
                .orElseThrow(() -> {
                    logger.warn("Login failed: User not found for email {}", loginRequestDTO.emailId());
                    return new IllegalArgumentException("User not found");
                });

        if (!user.isEmailVerified()) {
            logger.warn("Login failed: Email not verified for user {}", loginRequestDTO.emailId());
            throw new IllegalArgumentException("Email not verified");
        }

        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmailId(), loginRequestDTO.password()));
        if (!authentication.isAuthenticated()) {
            logger.warn("Login failed: Invalid credentials for user {}", loginRequestDTO.emailId());
            throw new IllegalArgumentException("Invalid Credentials");
        }

        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);
        List<RefreshToken> existingRefreshTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        existingRefreshTokens.forEach(token -> { token.setRevoked(true); refreshTokenRepository.save(token); });

        String accessToken = jwtService.generateAccessToken(user.getEmailId());
        RefreshToken refreshToken = jwtService.generateRefreshToken(user.getId());
        logger.info("User {} logged in successfully", user.getEmailId());
        return userMapper.toLoginResponseDTO(user, accessToken, refreshToken.getToken());
    }

    /**
     * Logout a user by revoking their refresh token and disconnecting them from the system.
     * The user is found in the database using emailId extracted from the JWT token.
     * Marked as transactional to keep the method atomic due to multiple database operations
     * @param authHeader The authorization header containing the JWT token
     */
    @Transactional
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String emailId = jwtService.extractUserName(token);
            Optional<User> userOptional = userRepository.findByEmailId(emailId);
            userOptional.ifPresent(user -> {
                Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUserAndRevokedFalse(user).stream().findFirst();
                refreshTokenOptional.ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
                userService.disconnect(user.getId());
                logger.info("User {} logged out", user.getEmailId());
            });
        }
    }

    /**
     * The refresh token is validated and if valid, a new access token is generated.
     * @param refreshTokenStr The refresh token string
     * @return The access token response DTO containing the new access token
     */
    public AccessTokenResponseDTO refreshAccessToken(String refreshTokenStr){
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr);
        if(refreshTokenOptional.isEmpty() || refreshTokenOptional.get().getExpiryDate().isBefore(java.time.Instant.now())){
            logger.warn("Refresh token invalid or expired: {}", refreshTokenStr);
            throw new RuntimeException("Invalid or expired refresh token");
        }
        User user = refreshTokenOptional.get().getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getEmailId());
        logger.info("Access token refreshed for user {}", user.getEmailId());
        return new AccessTokenResponseDTO(newAccessToken);
    }

    /**
     * Request for changing the password of the user.
     * The user is found in the database using emailId.
     * The old password is verified and if correct, the new password is set.
     * Marked as transactional to keep the method atomic due to multiple database operations
     * @param request The object containing emailId, oldPassword and newPassword
     */
    public void changePassword(PasswordChangeRequestDTO request){
        User user = userRepository.findByEmailId(request.emailId())
                .orElseThrow(() -> {
                    logger.warn("Password change failed: User not found for email {}", request.emailId());
                    return new IllegalArgumentException("User not found with the emailId" + request.emailId());
                });

        if(!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            logger.warn("Password change failed: Incorrect old password for user {}", request.emailId());
            throw new IllegalArgumentException("Old Password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        logger.info("Password changed for user {}", user.getEmailId());
    }

    /**
     * Send an email verification link to the user after registration.
     * The user is found in the database using emailId.
     * A verification token is generated and saved in the database.
     * An email is sent to the user with the verification link.
     * @param token token for email verification
     */
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Email verification failed: Invalid token {}", token);
                    return new IllegalArgumentException("Invalid or expired email verification token");
                });

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Email verification failed: Token expired {}", token);
            throw new IllegalArgumentException("Email verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(verificationToken);
        logger.info("Email verified for user {}", user.getEmailId());
    }

    /**
     * Reset the password using the provided token and new password.
     * The token is validated and if valid, the user's password is updated.
     * The reset token is deleted after successful password reset.
     * @param token The password reset token
     * @param newPassword The new password to set
     */
    @Transactional
    public void resetPassword(String token, String newPassword){
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Password reset failed: Invalid token {}", token);
                    return new IllegalArgumentException("Invalid or expired Password Reset Token");
                });
        if (resetToken.getExpiryDate().isBefore(Instant.now())){
            logger.warn("Password reset failed: Token expired {}", token);
            throw new IllegalArgumentException("Password reset token has expired");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
        logger.info("Password reset for user {}", user.getEmailId());
    }

    /**
     * Send a password reset link to the user's email.
     * The user is found in the database using emailId.
     * A reset token is generated and saved in the database.
     * An email is sent to the user with the reset link.
     * @param emailId The email address of the user
     */
    public void sendPasswordResetLink(@NotEmpty String emailId) {
        User user = userRepository.findByEmailId(emailId)
                .orElseThrow(() -> {
                    logger.warn("Password reset link request failed: User not found for email {}", emailId);
                    return new IllegalArgumentException("User not found with the given emailId");
                });
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(300);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setExpiryDate(expiry);
        resetToken.setUser(user);
        resetTokenRepository.save(resetToken);

        String resetLink = "http://localhost:8080/auth/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmailId(), resetLink);
        logger.info("Password reset link sent to {}", user.getEmailId());
    }
}

