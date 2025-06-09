package com.yaplab.config;

import com.yaplab.security.JWTFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration class for the YapLab application.
 * This class configures security settings, including authentication and authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig{

    /**
     * Constructor based dependency injection
     */
    private final UserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JWTFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    /**
     * Configures the security filter chain for the application.
     * Spring security's default CSRF function is disabled.
     * Any request with login, register or password reset paths are authorized by default and permitted.
     * The session is stateless and a filter is added before this default security filter to manage JWT tokens.
     * and adds the JWT filter to the security chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/verify-email",
                                "/auth/forgot-password",
                                "/auth/reset-password")
                        .permitAll()
                        .anyRequest().authenticated())
                        .sessionManagement(session -> session.
                        sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures CORS settings.
     * Allows requests from the specified origin with common HTTP methods and headers.
     * @return a CorsConfigurationSource instance
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bean for password encoding using BCrypt.
     * This encoder is used to hash passwords securely.
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(12);
    };

    /**
     * Bean for authentication manager.
     * This manager is used to handle authentication requests.
     * @param configuration the authentication configuration
     * @return an AuthenticationManager instance
     * @throws Exception if an error occurs during authentication manager creation
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Bean for DAO(Data Access Object) authentication provider.
     * This provider uses the user details service and password encoder for authentication.
     * @return an AuthenticationProvider instance
     */
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
}