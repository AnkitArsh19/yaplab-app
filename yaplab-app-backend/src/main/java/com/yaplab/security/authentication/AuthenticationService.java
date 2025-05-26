package com.yaplab.security.authentication;

import com.resend.core.exception.ResendException;
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
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public AuthenticationService(JWTService jwtService, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, AuthenticationManager authManager, UserMapper userMapper, PasswordEncoder passwordEncoder, PasswordResetTokenRepository resetTokenRepository, EmailService emailService, EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authManager = authManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenRepository = resetTokenRepository;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    /**
     * Login a user to the existing account.
     * The user is found in the database using emailId.
     * Authentication object is created by authManager by checking the password and username.
     * The status is updated to online and token is generated to send in the response.
     * Marked as transactional to keep the method atomic due to multiple database operations
     * @param loginRequestDTO The object containing username and password
     * @return The login response DTO
     */
    @Transactional
    public LoginResponseDTO loginUser(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmailId(loginRequestDTO.emailId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmailId(), loginRequestDTO.password()));
        if (!authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Invalid Credentials");
        }
        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);
        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        existingTokens.forEach(token -> { token.setRevoked(true); refreshTokenRepository.save(token); });

        String accessToken = jwtService.generateAccessToken(user.getEmailId());
        RefreshToken refreshToken = jwtService.generateRefreshToken(user.getId());
        return userMapper.toLoginResponseDTO(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String emailId = jwtService.extractUserName(token); // This is actually the email
            Optional<User> userOptional = userRepository.findByEmailId(emailId);
            userOptional.ifPresent(user -> {
                Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUserAndRevokedFalse(user).stream().findFirst();
                refreshTokenOptional.ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);                });
            });
        }
    }

    public AccessTokenResponseDTO refreshAccessToken(String refreshTokenStr){
    Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr);
    if(refreshTokenOptional.isEmpty() || refreshTokenOptional.get().getExpiryDate().isBefore(java.time.Instant.now())){
        throw new RuntimeException("Invalid or expired refresh token");
    }
    User user = refreshTokenOptional.get().getUser();
    String newAccessToken = jwtService.generateAccessToken(user.getEmailId());
        return new AccessTokenResponseDTO(newAccessToken);
}

    public void changePassword(PasswordChangeRequestDTO request){
        User user = userRepository.findByEmailId(request.emailId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the emailId" + request.emailId()));
        if(!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            throw new IllegalArgumentException("Old Password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired email verification token"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Email verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(verificationToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword){
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired Password Reset Token"));
        if (resetToken.getExpiryDate().isBefore(Instant.now())){
            throw new IllegalArgumentException("Password reset token has expired");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    public void sendPasswordResetLink(@NotEmpty String emailId) throws ResendException {
        User user = userRepository.findByEmailId(emailId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the given emailId"));
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(300);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setExpiryDate(expiry);
        resetToken.setUser(user);
        resetTokenRepository.save(resetToken);

        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmailId(), resetLink);
    }
}

