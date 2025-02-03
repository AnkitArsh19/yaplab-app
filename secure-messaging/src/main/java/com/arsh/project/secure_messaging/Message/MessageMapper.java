package com.arsh.project.secure_messaging.Message;
import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.enums.MessageStatus;
import com.arsh.project.secure_messaging.enums.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapper {
    public Message toPersonal(User sender, User receiver, String content) {

        Message toPersonal = new Message();
        toPersonal.setSender(sender);
        toPersonal.setReceiver(receiver);
        toPersonal.setContent(content);
        toPersonal.setMessageType(MessageType.TEXT);
        toPersonal.setSoftDeleted(false);
        toPersonal.setMessageStatus(MessageStatus.SENT);
        toPersonal.setTimestamp(LocalDateTime.now());
        return toPersonal;
    }

    public Message toGroup(User sender,Groups name, String content){

        Message toGroup = new Message();
        toGroup.setSender(sender);
        toGroup.setGroup(name);
        toGroup.setContent(content);
        toGroup.setMessageType(MessageType.TEXT);
        toGroup.setSoftDeleted(false);
        toGroup.setMessageStatus(MessageStatus.SENT);
        toGroup.setTimestamp(LocalDateTime.now());
        return toGroup;
    }
}
