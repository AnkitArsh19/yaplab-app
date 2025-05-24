package com.yaplab.token;

import com.yaplab.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expiryDate", nullable = false)
    private Instant expiryDate;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public RefreshToken() {
    }

    public RefreshToken(Long id, String token, User user, Instant expiryDate) {
        Id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
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

    public static class Builder{
        private Long id;
        private String token;
        private Instant expiryDate;
        private User user;

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
        public RefreshToken build(){
            return new RefreshToken(id, token, user, expiryDate);
        }
    }

    public static Builder builder(){
        return new Builder();
    }
}
