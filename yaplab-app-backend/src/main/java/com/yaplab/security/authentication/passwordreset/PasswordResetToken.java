package com.yaplab.security.authentication.passwordreset;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expiryDate", nullable = false)
    private Instant expiryDate;

    @ManyToOne
    private User user;

    public PasswordResetToken() {
    }

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
