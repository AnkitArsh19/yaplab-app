package com.yaplab.security.authentication.passwordreset;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entity representing a password reset token.
 * This class is used to store the token, its expiry date, and the associated user.
 */
@Entity
public class PasswordResetToken {

    /**
     * Unique identifier for each reset token which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    /**
     * The token string used for password reset.
     */
    @Column(name = "token", nullable = false)
    private String token;

    /**
     * The expiry date of the token, after which it is no longer valid.
     */
    @Column(name = "expiryDate", nullable = false)
    private Instant expiryDate;

    /**
     * The user associated with this password reset token.
     * Many tokens for single user
     */
    @ManyToOne
    private User user;

    /**
     * Default constructor
     */
    public PasswordResetToken() {
    }

    /**
     * Parameterized constructor
     */
    public PasswordResetToken(Long id, User user, Instant expiryDate, String token) {
        Id = id;
        this.user = user;
        this.expiryDate = expiryDate;
        this.token = token;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
