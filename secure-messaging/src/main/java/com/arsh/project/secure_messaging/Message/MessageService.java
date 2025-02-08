package com.arsh.project.secure_messaging.Message;

import com.arsh.project.secure_messaging.Group.Groups;
import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.enums.MessageStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    private final MessageRepo messageRepo;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepo messageRepo, MessageMapper messageMapper) {
        this.messageRepo = messageRepo;
        this.messageMapper = messageMapper;
    }

    public void sendPersonalMessage(User sender, User receiver, String content){

        Message message = messageMapper.toPersonal(sender, receiver, content);
        messageRepo.save(message);
    }

    public void sendGroupMessage(User sender, Groups group, String content){

        Message groupmessage = messageMapper.toGroup(sender, group, content);
        messageRepo.save(groupmessage);
    }

    public List<Message> getMessageBetweenUsers(User sender, User receiver){

        return messageRepo.findBySenderAndReceiver(sender, receiver);
    }

    public List<Message> getMessageOfGroups(Groups group){

        return messageRepo.findByGroups(group);
    }

    public void updateMessageStatus(Long id, MessageStatus status){
        Message message = messageRepo.findById(id)
                .orElseThrow(()-> new RuntimeException("Message not found"));
        message.setMessageStatus(status);
        messageRepo.save(message);
    }

    public void softDeleteMessage(Long id){
        Message message = messageRepo.findById(id)
                .orElseThrow(()-> new RuntimeException("Message not found"));
        message.setSoftDeleted(true);
        messageRepo.save(message);
    }
}

