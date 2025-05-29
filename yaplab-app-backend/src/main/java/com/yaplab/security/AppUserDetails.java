package com.yaplab.security;

import com.yaplab.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation for Spring Security
 * This class implements UserDetails to provide user-specific information to the Spring Security framework.
 * It is used to represent the authenticated user in the security context.
 * It is an interface so we define all methods to create object
 */
public class AppUserDetails implements UserDetails {

    /**
     * The user entity associated with this UserDetails object.
     */
    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the authorities granted to the user.
     * In this implementation, no specific authorities are assigned.
     * @return A collection of GrantedAuthority objects representing the user's authorities.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
