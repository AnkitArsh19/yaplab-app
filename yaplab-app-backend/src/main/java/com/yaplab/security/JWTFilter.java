package com.yaplab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Component to implement a JWT filter
 * The filter is implemented once for every HTTP request.
 * JWTFilter is a filter that processes incoming HTTP requests to check for JWT tokens.
 */
@Component
public class JWTFilter extends OncePerRequestFilter {

    /**
     * Dependency injection
     */
    private final JWTService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JWTFilter(JWTService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * This method is called for every request to filter the JWT token.
     * It checks the Authorization header for a Bearer token, validates it, and sets the authentication in the security context.
     * @param request  The HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain to continue processing the request
     * @throws ServletException If an error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtService.extractUserName(token);
            } catch (Exception e) {
                logger.error("Failed to extract username from token", e);
            }
        }

        /*
         If the user is present in the header it checks if the user not authenticated.
         * If not authenticated, user details are extracted and token is validated.
         * If valid, creates an object of usernamePasswordAuthenticationToken and sets in spring security context that the user is validated.
         */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            /*
             * If the token is invalid or expired, it checks for a refresh token and attempts to refresh the access token.
             * Sends the new access token as a response in the header.
             * Another object of UsernamePasswordAuthenticationToken is created and sets in spring security context that the user is validated.
             * If refresh token is also expired the request is unauthorized
             */
            else {
                String refreshToken = getRefreshTokenFromRequest(request);
                if (refreshToken != null) {
                    jwtService.findRefreshTokenByToken(refreshToken)
                            .ifPresentOrElse(validRefreshToken -> {
                                if (validRefreshToken.getExpiryDate().isAfter(java.time.Instant.now())) {
                                    UserDetails userDetailsForRefresh = userDetailsService.loadUserByUsername(validRefreshToken.getUser().getEmailId());
                                    String newAccessToken = jwtService.generateAccessToken(userDetailsForRefresh.getUsername());
                                    response.setHeader("Authorization", "Bearer " + newAccessToken);
                                    UsernamePasswordAuthenticationToken authToken =
                                            new UsernamePasswordAuthenticationToken(userDetailsForRefresh, null, userDetailsForRefresh.getAuthorities());
                                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                    SecurityContextHolder.getContext().setAuthentication(authToken);
                                } else {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                }
                            }, () -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            });
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        }
        /*
         * After handling this the request is passed on to the filter chain.
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the refresh token from the request.
     * It checks both the header and cookies for the refresh token.
     * @param request The HTTP request
     * @return The refresh token if found, otherwise null
     */
    private String getRefreshTokenFromRequest(HttpServletRequest request) {
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (refreshToken != null) {
            return refreshToken;
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}