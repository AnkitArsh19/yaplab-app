package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface to manage Message entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages sent by a specific user in a chatroom.
     * Ordered by timestamp in ascending order.
     * @param chatroomId The ID of the chatroom
     * @return List of messages sent by the user in the chatroom
     */
    List<Message> findByChatroomChatroomIdAndSoftDeletedFalseOrderByTimestampAsc(String chatroomId);

    /**
     * Finds all messages sent by a specific user and having a specific status in a chatroom.
     * @param chatroomId The ID of the chatroom
     * @param messageStatus The status of the message (e.g., SENT, DELIVERED, READ)
     * @return List of messages sent by the user in the chatroom with the specified status
     */
    List<Message> findByChatroom_ChatroomIdAndMessageStatus(String chatroomId, MessageStatus messageStatus);

    /**
     * Deletes all messages in a specific chatroom.
     * @param chatroomId The ID of the chatroom
     */
    void deleteByChatroomChatroomId(String chatroomId);

    /**
     * Finds all messages in a chatroom that are not hidden for a specific user.
     * @param chatroomId The ID of the chatroom
     * @param userId The ID of the user
     * @return List of messages in the chatroom not hidden for the user
     */
    List<Message> findByChatroomChatroomIdAndSoftDeletedFalseAndHiddenForUsersNotContainingOrderByTimestampAsc(String chatroomId, Long userId);
    
    /**
     * Deletes all messages sent by a specific user.
     * @param userId The ID of the user
     */
    void deleteBySenderId(Long userId);
    
    /**
     * Finds all messages sent by a specific user.
     * @param userId The ID of the user
     * @return List of messages sent by the user
     */
    List<Message> findBySenderId(Long userId);
    
    /**
     * Deletes all messages where a specific user is the receiver.
     * @param userId The ID of the user
     */
    void deleteByReceiverId(Long userId);
}