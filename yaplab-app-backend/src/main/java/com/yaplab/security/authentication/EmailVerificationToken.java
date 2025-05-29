package com.yaplab.security.authentication;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entity used to store an email verification token
 */
@Entity
public class EmailVerificationToken {

    /**
     * Unique identifier for each token which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Randomly generated token used for verification
     */
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    /**
     * Expiry date of the token
     */
    @Column(name = "expiryDate", nullable = false)
    private Instant expiryDate;

    /**
     * User info of the user who requested for token
     * Many tokens can be sent to same user
     */
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    /**
     * Default constructor
     */
    public EmailVerificationToken() {
    }

    /**
     * Parameterized constructor
     */
    public EmailVerificationToken(String token, Instant expiryDate, User user) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.user = user;
    }

    /**
     * Getters and setters
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
