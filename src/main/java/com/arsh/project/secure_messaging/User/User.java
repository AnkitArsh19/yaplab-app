package com.arsh.project.secure_messaging.User;

import jakarta.persistence.*;

/**
 * User entity to store user details like username, email ID, mobile number and password.
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
     * The name of the user (cannot be null).
     */
    @Column(name = "username", nullable = false)
    private String userName;

    /**
     * The emailId of the user (cannot be null and is unique for all users).
     */
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;

    /**
     * The mobile no. of the user (cannot be null and is unique for all users).
     */
    @Column(name = "MobileNo", unique = true, nullable = false)
    private String mobileNo;

    /**
     * The password of the account (cannot be null).
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     *Default constructor.
     */
    public User() {
    }

    /**
     * Parameterized constructor.
     */

    public User(Long id, String userName, String emailId, String mobileNo, String password) {
        this.id = id;
        this.userName = userName;
        this.emailId = emailId;
        this.mobileNo = mobileNo;
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

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
