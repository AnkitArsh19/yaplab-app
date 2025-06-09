package com.yaplab.message;

import com.yaplab.enums.MessageType;
import com.yaplab.files.File;
import com.yaplab.group.Group;
import com.yaplab.user.User;
import com.yaplab.chatroom.ChatRoom;
import com.yaplab.enums.MessageStatus;
import org.springframework.stereotype.Service;

/**
 * Service layer to map messages from DTOs to entities and vice versa.
 */
@Service
public class MessageMapper {

    /**
     * Determines the message type based on the file type.
     * @param fileType The type of the file (e.g., "image/png", "video/mp4").
     * @return The corresponding MessageType.
     */
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
     * @param chatroom The chatroom in which the message is sent.
     * @param receiver The receiver of the message.
     * @param content  The content of the message.
     * @param file     The file attached to the message, if any.
     * @return The message entity with desired attributes.
     */
    public Message createPersonalMessage(ChatRoom chatroom, User sender, User receiver, String content, File file) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        return new Message(chatroom, sender, receiver, content, messageType, MessageStatus.SENT, null, file);
    }

    /**
     * Maps the parameters to a message entity group messages
     * @param chatroom The chatroom in which the message is sent.
     * @param sender   The sender of the message.
     * @param group    The group to which the message is sent.
     * @param content  The content of the message.
     * @return The message entity with desired attributes.
     */
    public Message createGroupMessage(ChatRoom chatroom, User sender, Group group, String content, File file) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        return new Message(chatroom, sender, group, content, messageType, MessageStatus.SENT, null, file);
    }

    /**
     * Maps the parameters to a message entity for a reply message
     * @param chatroom The chatroom the message belongs to.
     * @param sender The sender of the reply message.
     * @param content The content of the reply message.
     * @param file Optional file attached to the reply.
     * @param repliedToMessage The original message being replied to.
     * @return The message entity for the reply with desired attributes.
     */
    public Message createReplyMessage(ChatRoom chatroom, User sender, String content, File file, Message repliedToMessage) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        return new Message(chatroom, sender, (User) null, content, messageType, MessageStatus.SENT, repliedToMessage, file);
    }

    /**
     * Maps parameters to a message entity for a forwarded message.
     *
     * @param chatroom        The chatroom the message is being forwarded to.
     * @param sender          The sender of the forwarded message.
     * @param content         The content of the forwarded message.
     * @param file            Optional file attached to the original message.
     * @param originalMessage The original message being forwarded.
     * @return The message entity for the forwarded message.
     */
    public Message createForwardedMessage(ChatRoom chatroom, User sender, String content, File file, Message originalMessage) {
        MessageType messageType = (file != null) ? determineMessageTypeFromFileType(file.getFileType()) : MessageType.TEXT;
        Message forwardedMessage = new Message(chatroom, sender, (User) null, content, messageType, MessageStatus.SENT, originalMessage, file);
        forwardedMessage.setForwarded(true);
        return forwardedMessage;
    }

    /**
     * Converts a Message entity to a MessageResponseDTO.
     * Handles files and message replies
     * @param message The message entity to convert.
     * @return A MessageResponseDTO containing the message details.
     */
    public MessageResponseDTO toResponseDTO(Message message) {
        String fileUrl = null;
        String fileName = null;
        Long fileSize = null;
        Long uploadedByUserId = null;
        String uploadedByUserName = null;
        String fileType = null;
        MessageResponseDTO.RepliedToMessageDTO repliedToMessageDTO = null;

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

        if (message.getReplyTo() != null) {
            Message repliedToMessage = message.getReplyTo();
            Long repliedToId = repliedToMessage.getId();
            String repliedToSenderName = (repliedToMessage.getSender() != null) ? repliedToMessage.getSender().getUserName() : null;
            String repliedToContent = repliedToMessage.getContent();

            repliedToMessageDTO = new MessageResponseDTO.RepliedToMessageDTO(repliedToId, repliedToSenderName, repliedToContent);
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
                fileType,
                repliedToMessageDTO,
                message.getChatroom().getChatroomId(),
                false,
                false,
                message.getEditTimestamp()
        );
    }
}
