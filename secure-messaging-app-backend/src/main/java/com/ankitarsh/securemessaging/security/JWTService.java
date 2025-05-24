package com.ankitarsh.securemessaging.security;

import com.ankitarsh.securemessaging.token.RefreshToken;
import com.ankitarsh.securemessaging.token.RefreshTokenRepository;
import com.ankitarsh.securemessaging.user.User;
import com.ankitarsh.securemessaging.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
public class JWTService {

    private final UserRepository userRepository;
    private final String secretKey;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    public JWTService(@Value("${jwt.secret}") String secretKey, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.secretKey = secretKey;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateToken(String userName) {
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

    public RefreshToken generateRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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

    // Method to delete a refresh token (for logout)
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    private SecretKey getKey() {
        byte[] byteKey = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(byteKey);
    }

    public String extractUserName(String token) {
        // extract the username from jwt token
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

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
