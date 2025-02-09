package com.arsh.project.secure_messaging.Message;

import com.arsh.project.secure_messaging.Group.Groups;
import com.arsh.project.secure_messaging.User.User;
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
     * Retrieves a list of messages exchanged between a specific sender and receiver.
     *
     * @param sender   The sender of the messages.
     * @param receiver The receiver of the messages.
     * @return List of messages exchanged between the two users.
     */
    List<Message> findBySenderAndReceiver(User sender, User receiver);

    /**
     * Retrieves all messages sent in a specific group.
     *
     * @param group The group whose messages are to be fetched.
     * @return List of messages in the group.
     */
    List<Message> findByGroup(Groups group);
}
