package com.yaplab.security.authentication;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entity used to store email verification tokens for both registration and email change verification
 */
@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {

    /**
     * Enum to differentiate between token types
     */
    public enum TokenType {
        REGISTRATION,
        EMAIL_CHANGE
    }

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
     * Type of token - either for registration or email change
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type")
    private TokenType tokenType;

    /**
     * New email address for email change tokens (null for registration tokens)
     */
    @Column(name = "new_email")
    private String newEmail;

    /**
     * User info of the user who requested for token
     * Many tokens can be sent to same user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    /**
     * Default constructor
     */
    public EmailVerificationToken() {
    }

    /**
     * Constructor for registration verification tokens
     */
    public EmailVerificationToken(String token, Instant expiryDate, User user) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.user = user;
        this.tokenType = TokenType.REGISTRATION;
        this.newEmail = null;
    }

    /**
     * Constructor for email change verification tokens
     */
    public EmailVerificationToken(String token, Instant expiryDate, User user, String newEmail) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.user = user;
        this.tokenType = TokenType.EMAIL_CHANGE;
        this.newEmail = newEmail;
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

    public TokenType getTokenType() {
        // Default to REGISTRATION if tokenType is null
        return tokenType != null ? tokenType : TokenType.REGISTRATION;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}
