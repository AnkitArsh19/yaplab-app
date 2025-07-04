package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import com.yaplab.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity to track per-user status for group messages.
 * This allows us to determine when ALL users have received/read a group message.
 */
@Entity
@Table(name = "user_message_status", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}))
public class UserMessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The message this status refers to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    /**
     * The user this status refers to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The status of this message for this specific user.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    /**
     * Timestamp when this status was last updated.
     */
    @Column(nullable = false)
    @CreationTimestamp
    private Instant updatedAt;

    /**
     * Default constructor.
     */
    public UserMessageStatus() {}

    /**
     * Parameterized constructor
     */
    public UserMessageStatus(Message message, User user, MessageStatus status) {
        this.message = message;
        this.user = user;
        this.status = status;
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

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
