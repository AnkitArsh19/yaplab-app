package com.ankitarsh.securemessaging.Message;

import com.ankitarsh.securemessaging.Group.Groups;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.enums.MessageStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for handling message-related operations such as sending personal/group message.
 * Get list of messages for personal/group chats, etc.
 * sending, retrieving, updating status, and soft deleting messages.
 */
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
    }

    /**
     * Sends a personal message from one user to another and saves it in the database.
     *
     * @param sender   The user sending the message.
     * @param receiver The user receiving the message.
     * @param content  The message content.
     */
    public void sendPersonalMessage(User sender, User receiver, String content){

        Message message = messageMapper.toPersonal(sender, receiver, content);
        messageRepository.save(message);
    }

    /**
     * Sends a group message from one user to a group with multiple users and saves it in the database.
     *
     * @param sender   The user sending the message.
     * @param group The group receiving the message.
     * @param content  The message content.
     */
    public void sendGroupMessage(User sender, Groups group, String content){

        Message groupmessage = messageMapper.toGroup(sender, group, content);
        messageRepository.save(groupmessage);
    }

    /**
     * Returns a list of messages between two users where one is sender and another is receiver.
     *
     * @param sender   The user sending the message.
     * @param receiver The user receiving the message.
     * @return A list of messages exchanged between the two users.
     */
    public List<Message> getMessageBetweenUsers(User sender, User receiver){

        return messageRepository.findBySenderAndReceiver(sender, receiver);
    }

    /**
     * Returns a list of messages sent in a group.
     *
     * @param group   The group where the conversation has occurred.
     * @return A list of messages in the group.
     */
    public List<Message> getMessageOfGroups(Groups group){

        return messageRepository.findByGroup(group);
    }

    /**
     * Updates the status of a message.
     *
     * @param id     The ID of the message.
     * @param status The status the message.
     * @throws RuntimeException if message is not found.
     */
    public void updateMessageStatus(Long id, MessageStatus status){
        Message message = messageRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Message not found"));
        message.setMessageStatus(status);
        messageRepository.save(message);
    }

    /**
     * Sends a personal message from one user to another and saves it in the database.
     *
     * @param id   The ID of the message.
     * @throws RuntimeException if message is not found.
     */
    public void softDeleteMessage(Long id){
        Message message = messageRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Message not found"));
        message.setSoftDeleted(true);
        messageRepository.save(message);
    }
}
