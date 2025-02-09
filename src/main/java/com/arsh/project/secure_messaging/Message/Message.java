package com.arsh.project.secure_messaging.Message;
import com.arsh.project.secure_messaging.Group.Groups;
import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.enums.MessageStatus;
import com.arsh.project.secure_messaging.enums.MessageType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Message")
public class Message {

    //Needs unique id for retrieval and tracking. Long is preferred for large datasets.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Keeps a track of who sent the message
    @ManyToOne
    @JoinColumn(name = "senderID", nullable = false)
    private User sender;

    //Keeps the track of receiver, can be null for group messages
    @ManyToOne(optional = true)
    @JoinColumn(name = "receiverID")
    private User receiver;

    //Stores group info for group messages
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Groups group;

    //Stores the content of the message in string format
    @Column(nullable = false)
    private String content;

    //Stores the type of message which can be different in different situations like text, images, audio, video
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    //Stores the exact time for the message sent
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    //Soft delete option to delete messages but with an option to recover deleted messages
    @Column(nullable = false)
    private Boolean softDeleted = false;

    //Stores the current status for the message sent like sent, read or delivered
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus messageStatus;

    //Stores the id the message for replying feature
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "replyTo_id")
    private Message replyTo;

    //Default Constructor
    public Message() {
    }

    // One-to-One Message Constructor for personal chats
    public Message(User sender, User receiver, String content, MessageType messageType,
                   MessageStatus messageStatus, Message replyTo) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
        this.replyTo = replyTo;
    }

    // Group Message Constructor for group messages
    public Message(User sender, Groups group, String content, MessageType messageType,
                   MessageStatus messageStatus, Message replyTo) {
        this.sender = sender;
        this.group = group;
        this.content = content;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
        this.replyTo = replyTo;
    }

    //Getters and setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public Groups getGroup() {
        return group;
    }

    public void setGroup(Groups group) {
        this.group = group;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public LocalDateTime getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public Message getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Message replyTo) {
        this.replyTo = replyTo;
    }
}
