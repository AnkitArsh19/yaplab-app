package com.yaplab.user;

import com.yaplab.enums.UserStatus;
import com.yaplab.security.token.RefreshToken;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * Represents a user in the system.
 * This entity is used to store user details
 */
@Entity
@Table(name = "user_details")
public class User {

    /**
     * Unique identifier for each user which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The userName of the user (cannot be null).
     */
    @Column(name = "userName", nullable = false)
    private String userName;

    /**
     * The emailId of the user (cannot be null and is unique for all users).
     */
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;

    /**
     * The mobile number of the user (cannot be null and is unique for all users).
     */
    @Column(name = "mobile_number", unique = true, nullable = false)
    private String mobileNumber;

    /**
     * The password of the account (cannot be null).
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * New field to track if the user's email is verified.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * New timestamp fields for auditing.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Updated timestamp field.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Status of the user(ONLINE or OFFLINE).
     */
    @Column(name = "user_status")
    private UserStatus status;

    /**
     * Url of the profile picture stored.
     */
    @Column(name = "profile-picture-url")
    private String profilePictureUrl;

    /**
     * List of valid and invalid tokens for security and maintaining record
     */
    @OneToMany(mappedBy = "user")
    private List<RefreshToken> refreshTokens;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Parameterized constructor.
     */
    public User(Long id, String userName, String emailId, String mobileNumber, String password) {
        this.id = id;
        this.userName = userName;
        this.emailId = emailId;
        this.mobileNumber = mobileNumber;
        this.password = password;
    }


     /**
     *  Getters and Setters for returning the details and setting the details for the private fields
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public List<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }

    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }
}