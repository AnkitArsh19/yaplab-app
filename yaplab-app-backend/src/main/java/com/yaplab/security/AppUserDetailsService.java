package com.yaplab.security;

import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service for loading user details
 * This service implements UserDetailsService to load user-specific data.
 * It retrieves user information from the UserRepository based on the email ID.
 * UserDetailsService is also an interface so we define how to load user data to create an object
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    /**
     * Logger for AuthUserDetailsService
     * This logger is used to log various events and errors in the AuthUserDetailsService class.
     * It helps in debugging and tracking the flow of operations related to auth user details management.
     */
    private static final Logger logger = LoggerFactory.getLogger(AppUserDetailsService.class);

    /**
     * Dependency injection
     */
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by email ID
     * This method retrieves a user from the repository based on the provided email ID.
     * @param emailId The email ID of the user to be loaded.
     * @return UserDetails object containing user information.
     */
    @Override
    public UserDetails loadUserByUsername(String emailId) throws UsernameNotFoundException {
        User user = userRepository.findByEmailId(emailId)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", emailId);
                    return new IllegalArgumentException("User not found with the emailId: " + emailId);
                });
        return new AppUserDetails(user);

    }
}
