package com.arsh.project.secure_messaging.Message;

import com.arsh.project.secure_messaging.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {
    //Fetches the list of messages between the specific sender and receiver
    List<Message> findBySenderAndReceiver(User sender, User receiver);
    //Fetches the list of messages in the specific group
    List<Message> findByGroups(Groups group);
}
