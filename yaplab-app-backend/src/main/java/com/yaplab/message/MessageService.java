package com.yaplab.message;

import com.yaplab.chatroom.ChatRoom;
import com.yaplab.chatroom.ChatRoomDTO;
import com.yaplab.chatroom.ChatRoomResponseDTO;
import com.yaplab.chatroom.ChatRoomService;
import com.yaplab.enums.ChatRoomType;
import com.yaplab.enums.MessageStatus;
import com.yaplab.files.File;
import com.yaplab.files.FilesRepository;
import com.yaplab.group.Group;
import com.yaplab.group.GroupService;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Service layer for handling message-related operations such as sending personal/group message.
 * Get list of messages for personal/group chats, etc.
 */
@Service
public class MessageService {

    /**
     * Constructor based dependency injection
     */
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
     * Creates a chatroom DTO with the list of participants to get or create a chatroomResponse DTO
     * Sends a file if a file is sent.
     * Creates a message and saves it
     */
    @Transactional
    public MessageResponseDTO sendPersonalMessage(MessageDTO messageDTO) {
        if (messageDTO.receiverId() == null || messageDTO.groupId() != null) {
            throw new IllegalArgumentException("For personal messages, receiverId must be present and groupId must be null.");
        }

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                null,
                ChatRoomType.PERSONAL,
                null,
                Arrays.asList(messageDTO.senderId(), messageDTO.receiverId())
        );

        ChatRoomResponseDTO chatRoomResponse = chatRoomService.getOrCreatePersonalChatRoom(chatRoomDTO);

        ChatRoom chatRoom = chatRoomService.getChatRoomById(chatRoomResponse.chatroomId())
                .orElseThrow(() -> new RuntimeException("Chatroom not found after creation/retrieval"));

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
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Sends a group message from one user to a group with multiple users and saves it in the database.
     * Creates a chatroom DTO with the list of participants to get or create a chatroomResponse DTO
     * Sends a file if a file is sent.
     * Creates a message and saves it
     */
    @Transactional // Add Transactional annotation
    public MessageResponseDTO sendGroupMessage(MessageDTO messageDTO) {
        if (messageDTO.groupId() == null || messageDTO.receiverId() != null) {
            throw new IllegalArgumentException("For group messages, groupId must be present and receiverId must be null.");
        }

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                null,
                ChatRoomType.GROUP,
                messageDTO.groupId(),
                null
        );

        ChatRoomResponseDTO chatRoomResponse = chatRoomService.getOrCreateGroupChatRoom(chatRoomDTO);

        ChatRoom chatRoom = chatRoomService.getChatRoomById(chatRoomResponse.chatroomId())
                .orElseThrow(() -> new RuntimeException("Chatroom not found after creation/retrieval"));

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

        Message message = messageMapper.createGroupMessage(chatRoom, sender, group, messageDTO.content(), attachedFile);
        messageRepository.save(message);
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Sends a reply message to an existing message and saves it in the database.
     * Sends a file as a file if user wants to send a file as a reply.
     */
    @Transactional
    public MessageResponseDTO sendReplyMessage(MessageDTO replyMessageDTO, Long repliedToMessageId) {
        if (replyMessageDTO.receiverId() != null || replyMessageDTO.groupId() != null) {
            throw new IllegalArgumentException("Invalid MessageDTO for a reply message. receiverId and groupId must be null.");
        }

        if (repliedToMessageId == null) {
            throw new IllegalArgumentException("repliedToMessageId cannot be null for a reply message.");
        }

        Message repliedToMessage = messageRepository.findById(repliedToMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Message being replied to not found with ID: " + repliedToMessageId));

        if (repliedToMessage.getSoftDeleted()) {
            throw new IllegalArgumentException("Cannot reply to a soft-deleted message.");
        }

        ChatRoom chatRoom = repliedToMessage.getChatroom();
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chatroom for the replied-to message not found.");
        }

        User sender = userService.getUserEntityByID(replyMessageDTO.senderId());

        File attachedFile = null;
        if (replyMessageDTO.fileUrl() != null && replyMessageDTO.fileName() != null && replyMessageDTO.fileSize() != null) {
            attachedFile = new File();
            attachedFile.setFileUrl(replyMessageDTO.fileUrl());
            attachedFile.setFileName(replyMessageDTO.fileName());
            attachedFile.setFileSize(replyMessageDTO.fileSize());
            attachedFile.setUploadedBy(sender);
            filesRepository.save(attachedFile);
        }


        Message replyMessage = messageMapper.createReplyMessage(
                chatRoom, sender, replyMessageDTO.content(),
                attachedFile, repliedToMessage
        );
        messageRepository.save(replyMessage);
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(replyMessage);
    }

    /**
     * Updates the status of a message.
     * @param id     The ID of the message.
     * @param status The status the message.
     */
    @Transactional
    public void updateMessageStatus(Long id, MessageStatus status){
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setMessageStatus(status);
        messageRepository.save(message);
    }

    /**
     * Soft deletes a message.
     * @param id   The ID of the message.
     */
    @Transactional
    public void softDeleteMessage(Long id){
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setSoftDeleted(true);
        messageRepository.save(message);
    }
}
