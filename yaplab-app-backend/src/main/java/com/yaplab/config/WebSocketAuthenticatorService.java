package com.yaplab.config;

import com.yaplab.security.AppUserDetails;
import com.yaplab.security.AppUserDetailsService;
import com.yaplab.security.JWTService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * WebSocketAuthenticatorService is a service that handles authentication for WebSocket connections.
 * It validates JWT tokens and retrieves user details to create an authenticated token.
 */
@Service
public class WebSocketAuthenticatorService {

    private final JWTService jwtService;
    private final AppUserDetailsService userDetailsService;

    public WebSocketAuthenticatorService(JWTService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Validates the JWT token and retrieves user details.
     * If the token is valid, it returns an authenticated UsernamePasswordAuthenticationToken.
     * If the token is invalid or expired, it throws an AuthenticationCredentialsNotFoundException.
     * @param jwt the JWT token to validate
     * @return an authenticated UsernamePasswordAuthenticationToken
     * @throws AuthenticationException if authentication fails
     */
    public UsernamePasswordAuthenticationToken getAuthenticatedOrFail(final String jwt) throws AuthenticationException {
        if (jwt == null || jwt.trim().isEmpty()) {
            throw new AuthenticationCredentialsNotFoundException("JWT token was null or empty.");
        }

        try {
            String email = jwtService.extractUserName(jwt);
            if (email == null) {
                throw new AuthenticationCredentialsNotFoundException("Could not extract email from JWT token.");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (userDetails instanceof AppUserDetails) {
                String userEmail = ((AppUserDetails) userDetails).getUser().getEmailId();
                boolean emailMatches = email.equals(userEmail);
                boolean tokenNotExpired = jwtService.isTokenExpired(jwt);

                if (!emailMatches || !tokenNotExpired) {
                    throw new AuthenticationCredentialsNotFoundException("JWT token validation failed.");
                }
                return new UsernamePasswordAuthenticationToken(
                        userEmail, // principal = email
                        null,
                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }

            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
            );

        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("JWT authentication failed: " + e.getMessage());
        }
    }
}