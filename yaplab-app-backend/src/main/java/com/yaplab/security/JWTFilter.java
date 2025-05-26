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

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JWTFilter(JWTService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

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
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Token is invalid, attempt to refresh
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
        filterChain.doFilter(request, response);
    }

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