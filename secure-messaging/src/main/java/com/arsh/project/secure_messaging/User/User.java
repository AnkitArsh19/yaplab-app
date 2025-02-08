package com.arsh.project.secure_messaging.User;

import jakarta.persistence.*;

//Create an Entity for users to store their login details
@Entity
//Table for the list of registered users
@Table(name = "User_details")
public class User {

    @Id
    //Generate new primary key for new id's by auto incrementing
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //A new user should have a username and other details that are mandatory and cannot be null
    @Column(name = "username", nullable = false)
    private String userName;
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;
    @Column(name = "MobileNo", unique = true, nullable = false)
    private String mobileNo;
    @Column(name = "password", nullable = false)
    private String password;

    //Default constructor
    public User() {
    }

    //Parameterized constructor

    public User(Long id, String userName, String emailId, String mobileNo, String password) {
        this.id = id;
        this.userName = userName;
        this.emailId = emailId;
        this.mobileNo = mobileNo;
        this.password = password;
    }


    //Getters and Setters for returning the details and setting the details for the private fields


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
