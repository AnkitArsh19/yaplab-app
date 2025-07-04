package com.yaplab.message;

import com.yaplab.chatroom.*;
import com.yaplab.enums.ChatRoomType;
import com.yaplab.files.File;
import com.yaplab.files.FilesRepository;
import com.yaplab.group.Group;
import com.yaplab.group.GroupService;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for handling message-related operations such as sending personal/group message.
 * Get list of messages for personal/group chats, etc.
 */
@Service
public class MessageService {

    /**
     * Logger for MessageService
     */
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    /**
     * Constructor based dependency injection
     */
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final GroupService groupService;
    private final ChatRoomRepository chatRoomRepository;
    private final FilesRepository filesRepository;
    private final MessageStatusService messageStatusService;

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper, ChatRoomService chatRoomService, UserService userService, GroupService groupService, ChatRoomRepository chatRoomRepository, FilesRepository filesRepository, MessageStatusService messageStatusService) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.chatRoomService = chatRoomService;
        this.userService = userService;
        this.groupService = groupService;
        this.chatRoomRepository = chatRoomRepository;
        this.filesRepository = filesRepository;
        this.messageStatusService = messageStatusService;
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
            logger.warn("Invalid MessageDTO for personal message: receiverId missing or groupId present. DTO: {}", messageDTO);
            throw new IllegalArgumentException("For personal messages, receiverId must be present and groupId must be null.");
        }

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                null,
                ChatRoomType.PERSONAL,
                null,
                // Converts ArrayList to List<String> for chatroom participants
                Arrays.asList(messageDTO.senderId(), messageDTO.receiverId())
        );

        ChatRoomResponseDTO chatRoomResponse = chatRoomService.getOrCreatePersonalChatRoom(chatRoomDTO);

        ChatRoom chatRoom = chatRoomService.getChatRoomById(chatRoomResponse.chatroomId())
                .orElseThrow(() -> {
                    logger.error("Chatroom not found after creation/retrieval for personal message between sender {} and receiver {}", messageDTO.senderId(), messageDTO.receiverId());
                    return new RuntimeException("Chatroom not found after creation/retrieval");
                });

        User sender = userService.getUserEntityByID(messageDTO.senderId());
        User receiver = userService.getUserEntityByID(messageDTO.receiverId());

        File attachedFile = null;
        if (messageDTO.fileId() != null) {
            attachedFile = filesRepository.findById(messageDTO.fileId())
                    .orElseThrow(() -> {
                        logger.error("File not found with ID: {} for personal message", messageDTO.fileId());
                        return new RuntimeException("File not found with ID: " + messageDTO.fileId());
                    });
        }

        // Handle new file upload
        else if (messageDTO.fileUrl() != null && messageDTO.fileName() != null && messageDTO.fileSize() != null) {
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
    @Transactional
    public MessageResponseDTO sendGroupMessage(MessageDTO messageDTO) {
        if (messageDTO.groupId() == null || messageDTO.receiverId() != null) {
            logger.warn("Invalid MessageDTO for group message: groupId missing or receiverId present. DTO: {}", messageDTO);
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
                .orElseThrow(() -> {
                    logger.error("Chatroom not found after creation/retrieval for group message in group {}", messageDTO.groupId());
                    return new RuntimeException("Chatroom not found after creation/retrieval");
                });

        User sender = userService.getUserEntityByID(messageDTO.senderId());
        Group group = groupService.getGroupEntity(messageDTO.groupId());

        File attachedFile = null;
        // Handle existing file reference (like downloaded GIFs)
        if (messageDTO.fileId() != null) {
            attachedFile = filesRepository.findById(messageDTO.fileId())
                    .orElseThrow(() -> {
                        logger.error("File not found with ID: {} for group message", messageDTO.fileId());
                        return new RuntimeException("File not found with ID: " + messageDTO.fileId());
                    });
        }
        else if (messageDTO.fileUrl() != null && messageDTO.fileName() != null && messageDTO.fileSize() != null) {
            attachedFile = new File();
            attachedFile.setFileUrl(messageDTO.fileUrl());
            attachedFile.setFileName(messageDTO.fileName());
            attachedFile.setFileSize(messageDTO.fileSize());
            attachedFile.setUploadedBy(sender);
            filesRepository.save(attachedFile);
        }

        Message message = messageMapper.createGroupMessage(chatRoom, sender, group, messageDTO.content(), attachedFile);
        messageRepository.save(message);
        
        // Initialize per-user status tracking for group messages
        messageStatusService.initializeGroupMessageStatuses(message);
        
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Get all messages for a room
     * @param roomId The room ID
     * @return List of MessageResponseDTO
     */
    public List<MessageResponseDTO> getRoomMessages(String roomId) {
        List<Message> messages = messageRepository.findByChatroomChatroomIdAndSoftDeletedFalseOrderByTimestampAsc(roomId);
        return messages.stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all messages for a room excluding messages hidden by a specific user
     * @param roomId The room ID
     * @param userId The user ID to filter hidden messages
     * @return List of MessageResponseDTO
     */
    public List<MessageResponseDTO> getRoomMessages(String roomId, Long userId) {
        List<Message> messages = messageRepository.findByChatroomChatroomIdAndSoftDeletedFalseAndHiddenForUsersNotContainingOrderByTimestampAsc(roomId, userId);
        return messages.stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Soft delete multiple messages
     * @param messageIds List of message IDs to delete
     * @param userId ID of the user requesting deletion
     * @return List of successfully deleted message IDs
     */
    @Transactional
    public List<Long> softDeleteMultipleMessages(List<Long> messageIds, Long userId) {
        List<Long> deletedIds = new ArrayList<>();

        for (Long messageId : messageIds) {
            try {
                Message message = messageRepository.findById(messageId)
                        .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));

                if (!message.getSender().getId().equals(userId)) {
                    logger.warn("User {} not authorized to delete message {}", userId, messageId);
                    continue;
                }
                message.setSoftDeleted(true);
                messageRepository.save(message);
                deletedIds.add(messageId);

            } catch (Exception e) {
                logger.error("Failed to delete message {}: {}", messageId, e.getMessage());
            }
        }
        return deletedIds;
    }

    /**
     * Forward multiple messages to a target room
     * @param messageIds List of message IDs to forward
     * @param targetRoomId Target room ID
     * @param senderId ID of the user forwarding
     * @return List of newly created forwarded messages
     */
    @Transactional
    public List<MessageResponseDTO> forwardMultipleMessages(List<Long> messageIds, String targetRoomId, Long senderId) {
        List<MessageResponseDTO> forwardedMessages = new ArrayList<>();

        ChatRoom targetRoom = chatRoomRepository.findById(targetRoomId)
                .orElseThrow(() -> new RuntimeException("Target room not found: " + targetRoomId));

        User sender = userService.getUserEntityByID(senderId);

        for (Long messageId : messageIds) {
            try {
                Message originalMessage = messageRepository.findById(messageId)
                        .orElseThrow(() -> new RuntimeException("Original message not found: " + messageId));

                Message forwardedMessage = messageMapper.createForwardedMessage(
                        targetRoom, sender, originalMessage.getContent(),
                        originalMessage.getFile(), originalMessage
                );

                // For personal chats, set the receiver
                if (targetRoom.getChatroomType() == com.yaplab.enums.ChatRoomType.PERSONAL) {
                    User receiver = targetRoom.getParticipants().stream()
                            .filter(participant -> !participant.getId().equals(senderId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Cannot find receiver in personal chat"));
                    forwardedMessage.setReceiver(receiver);
                }

                messageRepository.save(forwardedMessage);
                forwardedMessages.add(messageMapper.toResponseDTO(forwardedMessage));

            } catch (Exception e) {
                logger.error("Failed to forward message {}: {}", messageId, e.getMessage());
            }
        }

        chatRoomService.updateLastActivity(targetRoomId);
        
        return forwardedMessages;
    }

    /**
     * Edit a message with validation
     * @param messageId The ID of the message to edit
     * @param newContent The new content for the message
     * @param userId The ID of the user requesting the edit
     * @return The updated Message entity
     */
    @Transactional
    public Message editMessage(Long messageId, String newContent, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("User is not authorized to edit this message.");
        }

        if (message.getSoftDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message.");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditTimestamp(Instant.now());
        messageRepository.save(message);
        return message;
    }

    /**
     * Sends a reply message to an existing message and saves it in the database.
     * Sends a file as a file if user wants to send a file as a reply.
     */
    @Transactional
    public MessageResponseDTO sendReplyMessage(MessageDTO replyMessageDTO, Long repliedToMessageId) {
        if (replyMessageDTO.receiverId() != null || replyMessageDTO.groupId() != null) {
            logger.warn("Invalid MessageDTO for reply message: receiverId or groupId present. DTO: {}", replyMessageDTO);
            throw new IllegalArgumentException("Invalid MessageDTO for a reply message. receiverId and groupId must be null.");
        }

        if (repliedToMessageId == null) {
            logger.warn("Cannot send reply message: repliedToMessageId is null.");
            throw new IllegalArgumentException("repliedToMessageId cannot be null for a reply message.");
        }

        Message repliedToMessage = messageRepository.findById(repliedToMessageId)
                .orElseThrow(() -> {
                    logger.warn("Reply message failed: Message being replied to not found with ID: {}", repliedToMessageId);
                    return new IllegalArgumentException("Message being replied to not found with ID: " + repliedToMessageId);
                });

        if (repliedToMessage.getSoftDeleted()) {
            logger.warn("Reply message failed: Cannot reply to a soft-deleted message with ID: {}", repliedToMessageId);
            throw new IllegalArgumentException("Cannot reply to a soft-deleted message.");
        }

        if (repliedToMessage.getChatroom() == null) {
            logger.error("Chatroom not found for replied-to message with ID: {}", repliedToMessageId);
            throw new RuntimeException("Chatroom for the replied-to message not found.");
        }

        ChatRoom chatRoom = repliedToMessage.getChatroom();
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chatroom for the replied-to message not found.");
        }

        User sender = userService.getUserEntityByID(replyMessageDTO.senderId());

        File attachedFile = null;
        if (replyMessageDTO.fileId() != null) {
            attachedFile = filesRepository.findById(replyMessageDTO.fileId())
                    .orElseThrow(() -> {
                        logger.error("File not found with ID: {} for reply message", replyMessageDTO.fileId());
                        return new RuntimeException("File not found with ID: " + replyMessageDTO.fileId());
                    });
        }
        else if (replyMessageDTO.fileUrl() != null && replyMessageDTO.fileName() != null && replyMessageDTO.fileSize() != null) {
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
        
        // Initialize per-user status tracking if this is a group message reply
        if (replyMessage.getGroup() != null) {
            messageStatusService.initializeGroupMessageStatuses(replyMessage);
        }
        
        chatRoomService.updateLastActivity(chatRoom.getChatroomId());
        return messageMapper.toResponseDTO(replyMessage);
    }

    /**
     * Forwards a message to a different chat room.
     * @param messageId The ID of the message to forward.
     * @param recipientChatRoomId The ID of the chat room to forward the message to.
     * @param senderId The ID of the user forwarding the message.
     * @return The forwarded Message entity.
     */
    @Transactional
    public Message forwardMessage(Long messageId, String recipientChatRoomId, Long senderId) {
        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Original message not found with ID: " + messageId));

        ChatRoom recipientChatRoom = chatRoomRepository.findById(recipientChatRoomId)
                .orElseThrow(() -> new RuntimeException("Recipient chat room not found with ID: " + recipientChatRoomId));

        User sender = userService.getUserEntityByID(senderId);

        Message forwardedMessage = messageMapper.createForwardedMessage(
                recipientChatRoom, sender, originalMessage.getContent(), originalMessage.getFile(), originalMessage
        );

        // For personal chats, set the receiver
        if (recipientChatRoom.getChatroomType() == com.yaplab.enums.ChatRoomType.PERSONAL) {
            User receiver = recipientChatRoom.getParticipants().stream()
                    .filter(participant -> !participant.getId().equals(senderId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cannot find receiver in personal chat"));
            forwardedMessage.setReceiver(receiver);
        }

        messageRepository.save(forwardedMessage);
        
        // Initialize per-user status tracking if forwarding to a group
        if (recipientChatRoom.getChatroomType() == com.yaplab.enums.ChatRoomType.GROUP) {
            messageStatusService.initializeGroupMessageStatuses(forwardedMessage);
        }
        
        chatRoomService.updateLastActivity(recipientChatRoom.getChatroomId());
        return forwardedMessage;
    }

    /**
     * Soft delete a single message
     *
     * @param messageId ID of the message to delete
     * @param userId    ID of the user requesting deletion
     */
    @Transactional
    public void softDeleteSingleMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("User is not authorized to delete this message.");
        }

        message.setSoftDeleted(true);
        messageRepository.save(message);
    }

    /**
     * Get a message by ID
     * @param messageId The ID of the message
     * @return The message entity
     */
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));
    }

    /**
     * Convert Message entity to MessageResponseDTO
     * @param message The message entity
     * @return MessageResponseDTO
     */
    public MessageResponseDTO convertToResponseDTO(Message message) {
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Clear chat for a specific user by hiding all existing messages in the chatroom
     * @param chatroomId The ID of the chatroom
     * @param userId The ID of the user who wants to clear the chat
     */
    @Transactional
    public void clearChatForUser(String chatroomId, Long userId) {

        List<Message> messages = messageRepository.findByChatroomChatroomIdAndSoftDeletedFalseOrderByTimestampAsc(chatroomId);

        // Loop through messages and add userId to hiddenForUsers
        for (Message message : messages) {
            message.getHiddenForUsers().add(userId);
        }

        messageRepository.saveAll(messages);
    }
}