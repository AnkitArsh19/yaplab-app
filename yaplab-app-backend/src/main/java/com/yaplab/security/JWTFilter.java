package com.yaplab.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom filter for processing JWT tokens for every incoming HTTP request.
 * It extracts the token, validates it, and sets the authentication in the security context.
 * If the token is expired, it attempts to refresh it using a refresh token if available.
 */
@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JWTFilter(JWTService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Acts as a filter to authenticate users based on the JWT token provided in the request headers.
     * @param request the HTTP request containing the JWT token
     * @param response the HTTP response to be sent back
     * @param filterChain the filter chain to continue processing the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the refresh token and access token from the request headers or parameters
        String refreshTokenHeader = request.getHeader("X-Refresh-Token");
        String authHeader = request.getHeader("Authorization");
        String token = null;

        // Check for the Authorization header and extract the token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // If the token is not found in the Authorization header, check for a custom header
        // Websockets cannot set custom headers, so we check for a query parameter
        // Clients pass JWT token as a query parameter for WebSocket connections
        if (token == null && request.getRequestURI().startsWith("/ws") && request.getParameter("access_token") != null) {
            token = request.getParameter("access_token");
        }

        // If no token is found, continue the filter chain without authentication
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = null;
        boolean shouldRefresh = false;

        // Attempt to extract and validate the JWT token
        try {
            username = jwtService.extractUserName(token);

            // If the username is not null and no authentication is set in the security context,
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                boolean isTokenValid;

                // Check if the userDetails is an instance of AppUserDetails
                // Custom logic for AppUserDetails to validate the token and can be used in future for more complex user details
                if (userDetails instanceof AppUserDetails) {
                    String userEmail = ((AppUserDetails) userDetails).getUser().getEmailId();
                    boolean emailMatches = username.equals(userEmail);
                    boolean tokenNotExpired = jwtService.isTokenExpired(token);
                    // Validate the token based on email match and expiration
                    isTokenValid = emailMatches && tokenNotExpired;
                } else {
                    // For other UserDetails implementations, validate the token directly using generic JWTService
                    isTokenValid = jwtService.validateToken(token, userDetails);
                }

                // If the token is valid, set the authentication in the security context
                if (isTokenValid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    shouldRefresh = true;
                }
            }
        }

        // Handle the case where the JWT token has expired
        catch (ExpiredJwtException e) {
            shouldRefresh = true;
            try {
                username = e.getClaims().getSubject();
            } catch (Exception ignored) {
            }
        }

        // Handle any other exceptions that may occur during token processing
        // Set response status to 401 Unauthorized for WebSocket requests
        // or return a JSON error response for other requests
        catch (Exception e) {
            logger.error("Failed to process JWT token: {}", e.getMessage());
            if (request.getRequestURI().startsWith("/ws")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid token\",\"code\":\"INVALID_TOKEN\"}");
            }
            return;
        }

        // If the token is expired and a refresh token is provided, attempt to refresh the access token for requests not starting with "/ws"
        if (shouldRefresh && !request.getRequestURI().startsWith("/ws") &&
                refreshTokenHeader != null && !refreshTokenHeader.isEmpty()) {

            try {
                var refreshTokenOpt = jwtService.findRefreshTokenByToken(refreshTokenHeader);

                // Check if the refresh token is present, not expired, and not revoked
                if (refreshTokenOpt.isPresent() &&
                        refreshTokenOpt.get().getExpiryDate().isAfter(java.time.Instant.now()) &&
                        !refreshTokenOpt.get().isRevoked()) {

                    // Generate a new access token using the email from the refresh token
                    String userEmail = refreshTokenOpt.get().getUser().getEmailId();
                    String newAccessToken = jwtService.generateAccessToken(userEmail);

                    // Set the new access token in the response header
                    response.setHeader("Authorization", "Bearer " + newAccessToken);

                    // Load user details and set the authentication in the security context
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Set the details for the authentication token and update the security context
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Continue the filter chain with the refreshed token
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception refreshException) {
                logger.error("Failed to refresh token: {}", refreshException.getMessage());
            }
        }

        // Websocket requests that require authentication but have an expired token
        // should return a 401 Unauthorized status without a JSON response
        if (shouldRefresh) {
            if (request.getRequestURI().startsWith("/ws")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token expired\",\"code\":\"TOKEN_EXPIRED\"}");
            }
            return;
        }

        // Continue the filter chain for all other requests
        filterChain.doFilter(request, response);
    }
}