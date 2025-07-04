package com.yaplab.security;

import com.yaplab.security.token.RefreshToken;
import com.yaplab.security.token.RefreshTokenRepository;
import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Service for handling JWT operations such as token generation, validation, and refresh token management.
 */
@Service
public class JWTService {

    /**
     * The expiration time for refresh tokens in milliseconds.
     */
    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    /**
     * The expiration time for access tokens in milliseconds.
     */
    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    private final UserRepository userRepository;
    private final String secretKey;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Constructs a JWTService with the specified secret key and repositories.
     * @param secretKey the secret key used for signing JWTs
     */
    public JWTService(@Value("${jwt.secret}") String secretKey, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.secretKey = secretKey;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generates an access token for the specified user.
     * @param userName the username for which the access token is generated
     * Jwts builder is used to create a JWT with claims, subject, issued date, expiration date, and signature.
     * @return the generated access token as a String
     */
    public String generateAccessToken(String userName) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .and()
                .signWith(getKey())
                .compact();
    }

    /**
     * Generates a refresh token for the specified user.
     * @param userId the ID of the user for whom the refresh token is generated
     * This method first revokes any existing refresh tokens for the user, then creates a new refresh token with a unique UUID and an expiration date.
     * @return the generated refresh token as a RefreshToken object
     */
    public RefreshToken generateRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        existingTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(existingTokens);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .user(user)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findRefreshTokenByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public Optional<RefreshToken> findRefreshTokenById(Long id) {
        return refreshTokenRepository.findById(id);
    }

    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    private SecretKey getKey() {
        byte[] byteKey = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(byteKey);
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates the JWT token against the provided user details.
     * @param token the JWT token to validate
     * @param userDetails the user details to compare against
     * @return true if the token is valid and matches the user details, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUserName(token);
            return userName.equals(userDetails.getUsername()) && isTokenExpired(token);
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return !extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}