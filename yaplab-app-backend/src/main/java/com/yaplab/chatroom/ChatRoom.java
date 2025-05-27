package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.Group;
import com.yaplab.message.Message;
import com.yaplab.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Chatroom entity to store unique chatroom Id, list of participants, messages, participants, etc
 */
@Entity
@Table(name = "chat_room")
public class ChatRoom {

    /**
     * Unique identifier for each chatroom which is assigned automatically.
     * String is used to create uniques chatrooms based on users.
     */
    @Id
    @Column(nullable = false, updatable = false)
    private String chatroomId;

    /**
     * Type of the chatroom. Personal or group
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType chatroomType;

    /**
     * Creation time of chatroom
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Time when chatroom was last used
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant lastActivity;

    /**
     * One chatroom can belong to many users and one user can belong many chatrooms.
     * A hash set ensures unique elements and easy traversal. A chatroom will not have duplicate members.
     */
    @ManyToMany
    @JoinTable(
        name = "chatroom_participants",
        joinColumns = @JoinColumn(name = "chatroom_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    /**
     * One chatroom with many messages.
     * CascadeType.ALL ensures that all operations performed in chatroom applies to all messages as well
     */
    @OneToMany(mappedBy = "chatroom", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<>();


    /**
     * Chatrooms mapped to groups.
     */
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    /**
     * Default constructor required by JPA
     */
    public ChatRoom() {
    }

    /**
     * Constructor for personal chats between two users
     * @param chatroomId Unique identifier for the chatroom
     * @param sender First participant in the chat
     * @param receiver Second participant in the chat
     */
    public ChatRoom(String chatroomId, User sender, User receiver) {
        this.chatroomId = chatroomId;
        this.chatroomType = ChatRoomType.PERSONAL;
        this.participants = new HashSet<>();
        this.participants.add(sender);
        this.participants.add(receiver);
        this.messages = new ArrayList<>();
    }

    /**
     * Constructor for group chats
     * @param chatroomId Unique identifier for the chatroom
     * @param group The group this chatroom belongs to
     * @param participants Initial list of participants
     */
    public ChatRoom(String chatroomId, Group group, Set<User> participants) {
        this.chatroomId = chatroomId;
        this.chatroomType = ChatRoomType.GROUP;
        this.group = group;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
    }

    /**
     * Getters and setters
     */
    public String getChatroomId() {
        return chatroomId;
    }

    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public ChatRoomType getChatroomType() {
        return chatroomType;
    }

    public void setChatroomType(ChatRoomType chatroomType) {
        this.chatroomType = chatroomType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Instant lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
