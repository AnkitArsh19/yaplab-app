package com.ankitarsh.securemessaging.chatroom;

import com.ankitarsh.securemessaging.enums.ChatRoomType;
import com.ankitarsh.securemessaging.group.Group;
import com.ankitarsh.securemessaging.message.Message;
import com.ankitarsh.securemessaging.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "chat_room")
public class ChatRoom {
    
    @Id
    @Column(nullable = false, updatable = false)
    private String chatroomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ChatRoomType chatroomType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastActivity;

    @ManyToMany
    @JoinTable(
        name = "chatroom_participants",
        joinColumns = @JoinColumn(name = "chatroom_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @OneToMany(mappedBy = "chatroom", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<>();

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
     * 
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
     * 
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
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
