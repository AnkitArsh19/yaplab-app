package com.ankitarsh.securemessaging.message;

import com.ankitarsh.securemessaging.chatroom.ChatRoom;
import com.ankitarsh.securemessaging.enums.MessageStatus;
import com.ankitarsh.securemessaging.enums.MessageType;
import com.ankitarsh.securemessaging.files.File;
import com.ankitarsh.securemessaging.group.Group;
import com.ankitarsh.securemessaging.user.User;
import org.springframework.stereotype.Service;

/**
 * Service layer to map messages.
 */
@Service
public class MessageMapper {

    private MessageType determineMessageTypeFromFileType(String fileType) {
        if (fileType == null) return MessageType.TEXT;
        if (fileType.startsWith("image/")) return MessageType.IMAGE;
        if (fileType.startsWith("video/")) return MessageType.VIDEO;
        if (fileType.startsWith("audio/")) return MessageType.AUDIO;
        return MessageType.TEXT;
    }

    /**
     * Maps the parameters to a message entity for personal messages
     * @param sender   The sender of the message.
     * @param receiver The receiver of the message.
     * @param content  The content of the message.
     * @return The message entity with desired attributes.
     */
    public Message createPersonalMessage(ChatRoom chatroom, User sender, User receiver, String content, File file) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        return new Message(chatroom, sender, receiver, content, messageType, MessageStatus.SENT, null, file);
    }

    /**
     * Maps the parameters to a message entity group messages
     * @param sender   The sender of the message.
     * @param group    The userName of the group.
     * @param content  The content of the message.
     * @return The message entity with desired attributes.
     */
    public Message createGroupMessage(ChatRoom chatroom, User sender, Group group, String content, File file) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        return new Message(chatroom, sender, group, content, messageType, MessageStatus.SENT, null, file);
    }


        public MessageResponseDTO toResponseDTO(Message message) {
        String fileUrl = null;
        String fileName = null;
        Long fileSize = null;
        Long uploadedByUserId = null;
        String uploadedByUserName = null;
        String fileType = null;

        if (message.getFile() != null) {
            File file = message.getFile();
            fileUrl = file.getFileUrl();
            fileName = file.getFileName();
            fileSize = file.getFileSize();
            fileType = file.getFileType();

            if (file.getUploadedBy() != null) {
                uploadedByUserId = file.getUploadedBy().getId();
                uploadedByUserName = file.getUploadedBy().getUserName();
            }
        }

        return new MessageResponseDTO(
                message.getId(),
                message.getSender().getUserName(),
                message.getContent(),
                message.getTimestamp(),
                message.getMessageStatus(),
                fileUrl,
                fileName,
                fileSize,
                uploadedByUserId,
                uploadedByUserName,
                fileType
        );
    }
}
