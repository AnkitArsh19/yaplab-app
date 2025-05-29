package com.yaplab.security;

import com.yaplab.security.token.RefreshToken;
import com.yaplab.security.token.RefreshTokenRepository;
import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import com.yaplab.user.UserService;
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
 * JWTService is a service class that handles JWT token generation, validation, and refresh token management.
 * It provides methods to generate access tokens, refresh tokens, validate tokens, and extract user information from tokens.
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
     * This value is used to set the expiration time for the generated access tokens.
     */
    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    /**
     * Logger for JWTService
     * This logger is used to log various events and errors in the JWTService class.
     * It helps in debugging and tracking the flow of operations related to JWT management.
     */
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    /**
     * Dependency injection
     */
    private final UserRepository userRepository;
    private final String secretKey;
    private final RefreshTokenRepository refreshTokenRepository;

    public JWTService(@Value("${jwt.secret}") String secretKey, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.secretKey = secretKey;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generates an access token for the given username.
     * The token contains claims such as the username, issued at time, and expiration time.
     * The token is signed with our private secret key
     * @param userName The username for which the access token is generated.
     * @return A JWT access token as a String.
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
     * Generates a refresh token for the given user ID.
     * It first revokes any existing refresh tokens for the user, then builds a new refresh token with a unique token string and an expiration date.
     * @param userId UserId of the user
     * @return a new refresh token
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

    /**
     * This method retrieves a refresh token from the repository based on the provided token string.
     * @param token The token string of the refresh token to be found.
     * @return An Optional containing the RefreshToken if found, or empty if not found.
     */
    public Optional<RefreshToken> findRefreshTokenByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * This method retrieves a refresh token from the repository based on the provided ID.
     * @param id The ID of the refresh token to be found.
     * @return An Optional containing the RefreshToken if found, or empty if not found.
     */
    public Optional<RefreshToken> findRefreshTokenById(Long id) {
        return refreshTokenRepository.findById(id);
    }

    /**
     * This method removes the specified refresh token from the database.
     * @param refreshToken The RefreshToken object to be deleted.
     */
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    /**
     * Retrieves the secret key used for signing JWT tokens.
     * The secret key is decoded from a Base64 encoded string.
     * @return A SecretKey object representing the decoded secret key.
     */
    private SecretKey getKey() {
        byte[] byteKey = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(byteKey);
    }

    /**
     * This method retrieves the subject claim from the token, which is expected to be the username.
     * @param token The JWT token from which to extract the username.
     * @return The username extracted from the token.
     */
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * This method allows for extracting any claim from the token by providing a claim resolver function.
     * @param token The JWT token from which to extract the claim.
     * @param claimResolver A function that defines how to extract the desired claim from the Claims object.
     * @param <T> The type of the claim to be extracted.
     * @return The extracted claim value.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    /**
     * This method parses the token and retrieves all claims contained within it.
     * @param token The JWT token from which to extract all claims.
     * @return A Claims object containing all claims from the token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * This method checks if the token's username matches the user details and if the token is not expired.
     * @param token The JWT token to be validated.
     * @param userDetails The UserDetails object containing user information.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * This method compares the token's expiration date with the current date to determine if the token is still valid.
     * @param token The JWT token to be checked for expiration.
     * @return true if the token is expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * This method retrieves the expiration claim from the token, which indicates when the token will expire.
     * @param token The JWT token from which to extract the expiration date.
     * @return A Date object representing the expiration date of the token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
