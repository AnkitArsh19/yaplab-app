package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.Group;
import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface to manage Chatroom entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    /**
     * Find personal or group chatroom by the providing user details
     * @param user1 user object that can be sender or receiver
     * @param user2 another user object that can be sender or receiver
     * @param chatroomType Personal or group type
     * @return Chatroom object if it exists
     */
    Optional<ChatRoom> findByParticipantsContainingAndParticipantsContainingAndChatroomType(User user1, User user2, ChatRoomType chatroomType);

    /**
     * Find only the chatrooms associated with groups
     * @param group Group object to get the group
     * @param chatroomType Provided chatroom type to search only group chatrooms
     * @return Chatroom object if it exists
     */
    Optional<ChatRoom> findByGroupAndChatroomType(Group group, ChatRoomType chatroomType);

    /**
     * Find all chatrooms for a particular user
     * @param user The user object to get the user
     * @return list of chatrooms
     */
    List<ChatRoom> findAllByParticipantsContaining(User user);
}
