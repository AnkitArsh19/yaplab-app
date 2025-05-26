package com.yaplab.message;

import com.yaplab.chatroom.ChatRoom;
import com.yaplab.chatroom.ChatRoomService;
import com.yaplab.enums.MessageStatus;
import com.yaplab.enums.MessageType;
import com.yaplab.files.File;
import com.yaplab.files.FilesRepository;
import com.yaplab.group.Group;
import com.yaplab.group.GroupService;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for handling message-related operations such as sending personal/group message.
 * Get list of messages for personal/group chats, etc.
 * sending, retrieving, updating status, and soft deleting messages.
 */
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final GroupService groupService;
    private final FilesRepository filesRepository;

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper, ChatRoomService chatRoomService, UserService userService, GroupService groupService, FilesRepository filesRepository) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.chatRoomService = chatRoomService;
        this.userService = userService;
        this.groupService = groupService;
        this.filesRepository = filesRepository;
    }

    /**
     * Sends a personal message from one user to another and saves it in the database.
     * Sends a file if a file is sent.
     */
    public MessageResponseDTO sendPersonalMessage(
            MessageDTO messageDTO){
        if (messageDTO.receiverId() == null || messageDTO.groupId() != null || messageDTO.repliedToMessageId() != null) {
            throw new IllegalArgumentException("Invalid MessageDTO for a personal message. receiverId must be present, groupId and repliedToMessageId must be null.");
        }
        String chatRoomId = chatRoomService.getOrCreatePersonalChatRoomId(messageDTO.senderId(), messageDTO.receiverId());
        ChatRoom chatRoom = chatRoomService.getChatroomById(chatRoomId);
        User sender = userService.getUserEntityByID(messageDTO.senderId());
        User receiver = userService.getUserEntityByID(messageDTO.receiverId());
        File attachedFile = null;
        if (messageDTO.fileUrl() != null && messageDTO.fileName() != null && messageDTO.fileSize() != null) {
            attachedFile = new File();
            attachedFile.setFileUrl(messageDTO.fileUrl());
            attachedFile.setFileName(messageDTO.fileName());
            attachedFile.setFileSize(messageDTO.fileSize());
            attachedFile.setUploadedBy(sender);
            filesRepository.save(attachedFile);
        }
        Message message = messageMapper.createPersonalMessage(chatRoom, sender, receiver, messageDTO.content(), attachedFile);
        messageRepository.save(message);
        chatRoomService.updateLastActivity(chatRoomId);
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Sends a group message from one user to a group with multiple users and saves it in the database.
     * Sends a file if a file is sent
     */
    public MessageResponseDTO sendGroupMessage(
            MessageDTO messageDTO
    ){
        if (messageDTO.groupId() == null || messageDTO.receiverId() != null || messageDTO.repliedToMessageId() != null) {
        throw new IllegalArgumentException("Invalid MessageDTO for a group message. groupId must be present, receiverId and repliedToMessageId must be null.");
        }
        String chatRoomId = chatRoomService.getOrCreateGroupChatRoomId(messageDTO.groupId());
        ChatRoom chatRoom = chatRoomService.getChatroomById(chatRoomId);
        User sender = userService.getUserEntityByID(messageDTO.senderId());
        Group group = groupService.getGroupEntity(messageDTO.groupId());
        File attachedFile = null;
        if (messageDTO.fileUrl() != null && messageDTO.fileName() != null && messageDTO.fileSize() != null) {
            attachedFile = new File();
            attachedFile.setFileUrl(messageDTO.fileUrl());
            attachedFile.setFileName(messageDTO.fileName());
            attachedFile.setFileSize(messageDTO.fileSize());
            attachedFile.setUploadedBy(sender);
            filesRepository.save(attachedFile);
        }

        Message message = messageMapper.createGroupMessage(chatRoom, sender, group , messageDTO.content(), attachedFile);
        messageRepository.save(message);
        chatRoomService.updateLastActivity(chatRoomId);
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Sends a reply message to an existing message and saves it in the database.
     */
    public MessageResponseDTO sendReplyMessage(MessageDTO replyMessageDTO, Long repliedToMessageId) {
        if (replyMessageDTO.receiverId() != null || replyMessageDTO.groupId() != null) {
            throw new IllegalArgumentException("Invalid MessageDTO for a reply message. receiverId and groupId must be null.");
        }

        // Find the message being replied to
        if (repliedToMessageId == null) {
            throw new IllegalArgumentException("repliedToMessageId cannot be null for a reply message.");
        }

        Message repliedToMessage = messageRepository.findById(repliedToMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Message being replied to not found with ID: " + repliedToMessageId));

        if (repliedToMessage.getSoftDeleted()) {
            throw new IllegalArgumentException("Cannot reply to a soft-deleted message.");
        }

        // Determine the chatroom for the reply message
        ChatRoom chatRoom = repliedToMessage.getChatroom();
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chatroom for the replied-to message not found.");
        }

        // Get the sender of the reply message
        User sender = userService.getUserEntityByID(replyMessageDTO.senderId());

        // Create or fetch the File entity based on MessageDTO file information (similar to sendPersonalMessage/sendGroupMessage)
        File attachedFile = null;
        if (replyMessageDTO.fileUrl() != null && replyMessageDTO.fileName() != null && replyMessageDTO.fileSize() != null) {
            attachedFile = new File();
            attachedFile.setFileUrl(replyMessageDTO.fileUrl());
            attachedFile.setFileName(replyMessageDTO.fileName());
            attachedFile.setFileSize(replyMessageDTO.fileSize());
            attachedFile.setUploadedBy(sender);
            filesRepository.save(attachedFile);
        }

        // Create the reply message entity
        Message replyMessage = messageMapper.createReplyMessage(
                chatRoom, sender, replyMessageDTO.content(),
                attachedFile, repliedToMessage
        );
        messageRepository.save(replyMessage);
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(replyMessage);
    }

    /**
     * Returns a list of messages between two users where one is sender and another is receiver.
     * @return A list of messages exchanged between the two users.
     */
    public List<MessageResponseDTO> getMessageBetweenUsers(Long senderId, Long receiverId){
        String chatroomId = chatRoomService.getOrCreatePersonalChatRoomId(senderId, receiverId);
        return messageRepository.findByChatroom_ChatroomId(chatroomId)
                .stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of messages sent in a group.
     * @return A list of messages in the group.
     */
    public List<MessageResponseDTO> getMessageOfGroups(Long groupId){
        String chatroomId = chatRoomService.getOrCreateGroupChatRoomId(groupId);
        return messageRepository.findByChatroom_ChatroomId(chatroomId)
                .stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of a message.
     * @param id     The ID of the message.
     * @param status The status the message.
     * @throws RuntimeException if message is not found.
     */
    public void updateMessageStatus(Long id, MessageStatus status){
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setMessageStatus(status);
        messageRepository.save(message);
    }

    /**
     * Sends a personal message from one user to another and saves it in the database.
     * @param id   The ID of the message.
     * @throws RuntimeException if message is not found.
     */
    public void softDeleteMessage(Long id){
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setSoftDeleted(true);
        messageRepository.save(message);
    }
}
