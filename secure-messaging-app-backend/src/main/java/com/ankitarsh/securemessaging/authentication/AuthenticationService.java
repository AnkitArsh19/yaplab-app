package com.ankitarsh.securemessaging.authentication;

import com.ankitarsh.securemessaging.enums.UserStatus;
import com.ankitarsh.securemessaging.security.JWTService;
import com.ankitarsh.securemessaging.token.AccessTokenResponseDTO;
import com.ankitarsh.securemessaging.token.RefreshToken;
import com.ankitarsh.securemessaging.token.RefreshTokenRepository;
import com.ankitarsh.securemessaging.user.User;
import com.ankitarsh.securemessaging.user.UserMapper;
import com.ankitarsh.securemessaging.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authManager;
    private final UserMapper userMapper;

    public AuthenticationService(JWTService jwtService, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, AuthenticationManager authManager, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authManager = authManager;
        this.userMapper = userMapper;
    }

    /**
     * Login a user to the existing account.
     * The user is found in the database using emailId.
     * Authentication object is created by authManager by checking the password and username.
     * The status is updated to online and token is generated to send in the response
     * @param loginRequestDTO The object containing username and password
     * @return The login response DTO
     */
    public LoginResponseDTO loginUser(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmailId(loginRequestDTO.emailId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUserName(), loginRequestDTO.password()));
        if (!authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Invalid Credentials");
        }
        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);
        String accessToken = jwtService.generateToken(user.getEmailId());
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
                Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUser(user);
                refreshTokenOptional.ifPresent(refreshTokenRepository::delete);
            });
        }
    }

    public AccessTokenResponseDTO refreshAccessToken(String refreshTokenStr){
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByToken(refreshTokenStr);
        if(refreshTokenOptional.isEmpty() || refreshTokenOptional.get().getExpiryDate().isBefore(java.time.Instant.now())){
            throw new RuntimeException("Invalid or expired refresh token");
        }
        User user = refreshTokenOptional.get().getUser();
        String newAccessToken = jwtService.generateToken(user.getEmailId());
        return new AccessTokenResponseDTO(newAccessToken);
    }



}

