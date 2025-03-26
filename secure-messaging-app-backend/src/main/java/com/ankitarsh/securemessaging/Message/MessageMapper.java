package com.ankitarsh.securemessaging.Message;

import com.ankitarsh.securemessaging.ChatRoom.ChatRoom;
import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.enums.MessageStatus;
import com.ankitarsh.securemessaging.enums.MessageType;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * Service layer to map messages.
 */
@Service
public class MessageMapper {

    /**
     * Maps the parameters to a message entity for personal messages
     * @param sender   The sender of the message.
     * @param receiver The receiver of the message.
     * @param content  The content of the message.
     * @return The message entity with desired attributes.
     */
    public Message createPersonalMessage(ChatRoom chatroom, User sender, User receiver, String content) {
        return new Message(chatroom, sender, receiver, content, MessageType.TEXT, MessageStatus.SENT, null);
    }

    /**
     * Maps the parameters to a message entity group messages
     * @param sender   The sender of the message.
     * @param group    The userName of the group.
     * @param content  The content of the message.
     * @return The message entity with desired attributes.
     */
    public Message createGroupMessage(ChatRoom chatroom, User sender, Group group, String content) {
        return new Message(chatroom, sender, group, content, MessageType.TEXT, MessageStatus.SENT, null);
    }

    public MessageResponseDTO toResponseDTO(Message message){
        return new MessageResponseDTO(
                message.getId(),
                message.getSender().getUserName(),
                message.getContent(),
                message.getTimestamp(),
                message.getMessageStatus()
        );
    }
}
