package com.yaplab.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface to manage Groups entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages for a given chatroom that are not soft-deleted.
     * @param chatroomId The ID of the chatroom.
     * @return A list of non-soft-deleted messages in the chatroom.
     */
    List<Message> findByChatroom_ChatroomIdAndSoftDeletedFalse(String chatroomId);

}