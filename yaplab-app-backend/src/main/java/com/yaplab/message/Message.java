package com.yaplab.message;

import com.yaplab.chatroom.ChatRoom;
import com.yaplab.enums.MessageStatus;
import com.yaplab.enums.MessageType;
import com.yaplab.files.File;
import com.yaplab.group.Group;
import com.yaplab.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Message entity to store id, sender_id, receiver_id, content, message_type, message_status, etc.
 */
@Entity
@Table(name = "message")
public class Message {

    /**
     * Unique identifier for each message which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The chatroom this message belongs to.
     */
    @ManyToOne
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatroom;

    /**
     * The sender of the message (cannot be null).
     */
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The receiver of the message (nullable for group messages).
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    /**
     * The group to which the message belongs (nullable for personal messages).
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "group_id")
    private Group group;

    /**
     * Content of the message in text format.
     */
    @Column(nullable = true)
    private String content;

    /**
     * Type of the message. Can be Text, Image, Audio, Video.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private File file;

    /**
     * Timestamp of when message was created.
     */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    /**
     * Soft delete flag to allow message recovery.
     */
    @Column(nullable = false)
    private Boolean softDeleted = false;

    /**
     * Status of the message. Can be Sent, Delivered, Read.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus messageStatus;

    /**
     * If this message is a reply, it stores the referenced message
     * Cascade is used to avoid unintended deletions.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "replyTo_id")
    private Message replyTo;

    /**
     * Default Constructor.
     */
    public Message() {
    }

    /**
     * One-to-One Message Constructor for personal chats
     */
    public Message(ChatRoom chatroom, User sender, User receiver, String content, MessageType messageType,
                   MessageStatus messageStatus, Message replyTo, File file) {
        this.chatroom = chatroom;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.file = file;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
        this.replyTo = replyTo;
    }

    /**
     * Constructor for group messages
     */
    public Message(ChatRoom chatroom, User sender, Group group, String content, MessageType messageType,
                   MessageStatus messageStatus, Message replyTo, File file) {
        this.chatroom = chatroom;
        this.sender = sender;
        this.group = group;
        this.file = file;
        this.content = content;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
        this.replyTo = replyTo;
    }

    /**
     *  Getters and setters
     */
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

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ChatRoom getChatroom() {
        return chatroom;
    }

    public void setChatroom(ChatRoom chatroom) {
        this.chatroom = chatroom;
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
