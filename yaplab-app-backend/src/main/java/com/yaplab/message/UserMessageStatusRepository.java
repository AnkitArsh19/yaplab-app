package com.yaplab.message;

import com.yaplab.enums.MessageStatus;
import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserMessageStatus entity operations.
 */
@Repository
public interface UserMessageStatusRepository extends JpaRepository<UserMessageStatus, Long> {

    /**
     * Find the status of a specific message for a specific user.
     */
    Optional<UserMessageStatus> findByMessageIdAndUserId(Long messageId, Long userId);

    /**
     * Find all status entries for a specific message.
     */
    List<UserMessageStatus> findByMessageId(Long messageId);

    /**
     * Count how many users have a specific status for a given message.
     */
    @Query("SELECT COUNT(ums) FROM UserMessageStatus ums WHERE ums.message.id = :messageId AND ums.status = :status")
    long countByMessageIdAndStatus(@Param("messageId") Long messageId, @Param("status") MessageStatus status);
    
    /**
     * Delete all status entries for a specific user.
     */
    void deleteByUserId(Long userId);
    
    /**
     * Delete all status entries for messages in a specific chatroom.
     */
    @Modifying
    @Query("DELETE FROM UserMessageStatus ums WHERE ums.message.chatroom.chatroomId = :chatroomId")
    void deleteByMessageChatroomId(@Param("chatroomId") String chatroomId);
    
    /**
     * Delete all status entries for a specific user in a specific chatroom.
     */
    @Modifying
    @Query("DELETE FROM UserMessageStatus ums WHERE ums.user.id = :userId AND ums.message.chatroom.chatroomId = :chatroomId")
    void deleteByUserIdAndMessageChatroomId(@Param("userId") Long userId, @Param("chatroomId") String chatroomId);

    /**
     * Delete all status entries for messages sent by a specific user.
     * This deletes all user_message_status entries for messages where the specified user is the sender.
     */
    @Modifying
    @Query("DELETE FROM UserMessageStatus ums WHERE ums.message.sender.id = :senderId")
    void deleteByMessageSenderId(@Param("senderId") Long senderId);
    
    /**
     * Delete all status entries for messages received by a specific user.
     * This deletes all user_message_status entries for messages where the specified user is the receiver.
     */
    @Modifying
    @Query("DELETE FROM UserMessageStatus ums WHERE ums.message.receiver.id = :receiverId")
    void deleteByMessageReceiverId(@Param("receiverId") Long receiverId);
}
