package com.yaplab.chatroom;

/**
 * Represents a typing indicator message to be sent over WebSocket.
 */
public class TypingIndicatorMessage {

    /**
     * UserId of the user
     */
    private Long userId;

    /**
     * Username of the user
     */
    private String userName;

    /**
     * ChatroomId of the chatroom
     */
    private String chatRoomId;

    /**
     * Tells if the user is typing or not
     */
    private boolean isTyping;

    /**
     * Default constructor
     */
    public TypingIndicatorMessage() {
    }

    /**
     * Parameterized constructor
     */
    public TypingIndicatorMessage(Long userId, String userName, String chatRoomId, boolean isTyping) {
        this.userId = userId;
        this.userName = userName;
        this.chatRoomId = chatRoomId;
        this.isTyping = isTyping;
    }

    /**
     * Getters and setters
     */
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}
