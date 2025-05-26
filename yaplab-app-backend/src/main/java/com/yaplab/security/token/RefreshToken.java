package com.yaplab.security.token;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entity to store a refresh token
 */
@Entity
public class RefreshToken {

    /**
     * Unique identifier for each user which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    /**
     * The actual token that is stored
     */
    @Column(name = "token", nullable = false)
    private String token;

    /**
     * Expiry date of the token
     */
    @Column(name = "expiryDate", nullable = false)
    private Instant expiryDate;

    /**
     * Joined one to one with user entity
     * Each user can have only 1 refresh token
     */
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /**
     * Default constructor
     */
    public RefreshToken() {
    }

    /**
     * Parameterized constructor
     */
    public RefreshToken(Long id, String token, User user, Instant expiryDate, boolean revoked) {
        Id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.revoked = revoked;
    }

    /**
     * Getters and setters
     */
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * A builder class for creating an instance of Refresh token
     * We do not explicitly set or change the token or expiry date rather create an instance to return a new object everytime a refresh token is required.
     * Old tokens will be invalid and the system keeps a log of invalid tokens for security purposes.
     */
    public static class Builder{
        private Long id;
        private String token;
        private Instant expiryDate;
        private User user;
        private boolean revoked;

        public Builder id(Long id){
            this.id = id;
            return this;
        }

        public Builder token(String token){
            this.token = token;
            return this;
        }

        public Builder expiryDate(Instant expiryDate){
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder user(User user){
            this.user = user;
            return this;
        }

        public Builder revoked(boolean revoked){
            this.revoked = revoked;
            return this;
        }

        public RefreshToken build(){
            return new RefreshToken(id, token, user, expiryDate, revoked);
        }
    }

    public static Builder builder(){
        return new Builder();
    }
}
