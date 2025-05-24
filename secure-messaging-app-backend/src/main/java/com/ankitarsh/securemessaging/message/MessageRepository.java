package com.ankitarsh.securemessaging.message;

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
     * Finds list of messages by chatRoomId
     * @param chatroomId The chatRoomId.
     */
    List<Message> findByChatroom_ChatroomId(String chatroomId);

}