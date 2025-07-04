package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.Group;
import com.yaplab.group.GroupRepository;
import com.yaplab.message.MessageMapper;
import com.yaplab.message.MessageRepository;
import com.yaplab.message.MessageResponseDTO;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for handling chatroom related operations such as creating chatrooms, creating chatroomId, getting messages, adding participants, etc
 */
@Service
public class ChatRoomService {

    /**
     * Logger for ChatroomService
     * This logger is used to log various events and errors in the ChatroomService class.
     * It helps in debugging and tracking the flow of operations related to chatroom management.
     */
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);

    /**
     * Constructor based dependency injection
     */
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final MessageMapper messageMapper;
    private final MessageRepository messageRepository;

    public ChatRoomService(UserService userService, GroupRepository groupRepository, ChatRoomRepository chatRoomRepository, ChatRoomMapper chatRoomMapper, MessageMapper messageMapper, MessageRepository messageRepository) {
        this.userService = userService;
        this.groupRepository = groupRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMapper = chatRoomMapper;
        this.messageMapper = messageMapper;
        this.messageRepository = messageRepository;
    }

    /**
     * Finds or creates a personal chat room between two users based on ChatRoomDTO.
     * Creates a new one if the chatroom doesn't exist
     * @param chatRoomDTO DTO containing participant IDs.
     * @return ChatRoomResponseDTO
     */
    @Transactional
    public ChatRoomResponseDTO getOrCreatePersonalChatRoom(ChatRoomDTO chatRoomDTO) {
        if (chatRoomDTO.participantIds() == null || chatRoomDTO.participantIds().size() != 2) {
            logger.warn("Invalid ChatRoomDTO for personal chat: participantIds must contain exactly two user IDs.");
            throw new IllegalArgumentException("For personal chat, participantIds must contain exactly two user IDs.");
        }
        Long userId1 = chatRoomDTO.participantIds().get(0);
        Long userId2 = chatRoomDTO.participantIds().get(1);

        User user1 = userService.getUserEntityByID(userId1);
        User user2 = userService.getUserEntityByID(userId2);


        logger.debug("Attempting to find existing personal chatroom between users {} and {}", userId1, userId2);
        Optional<ChatRoom> existing = chatRoomRepository.findByParticipantsContainingAndParticipantsContainingAndChatroomType(user1, user2, ChatRoomType.PERSONAL);

        if (existing.isPresent()) {
            return chatRoomMapper.chatRoomResponseDTO(existing.get());
        }

        String chatRoomId = userId1 < userId2 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
        Set<User> participants = new HashSet<>(List.of(user1, user2));
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatroomId(chatRoomId);
        chatRoom.setChatroomType(ChatRoomType.PERSONAL);
        chatRoom.setParticipants(participants);
        chatRoom.setLastActivity(Instant.now());
        return chatRoomMapper.chatRoomResponseDTO(chatRoomRepository.save(chatRoom));
    }

    /**
     * Finds or creates a group chat room for a group based on ChatRoomDTO.
     * Uses repository method to find existing group chat.
     * Creates a new one if the chatroom doesn't exist
     * @param chatRoomDTO DTO containing the group ID.
     * @return ChatRoomResponseDTO
     */
    @Transactional
    public ChatRoomResponseDTO getOrCreateGroupChatRoom(ChatRoomDTO chatRoomDTO) {
        if (chatRoomDTO.groupId() == null) {
            logger.warn("Invalid ChatRoomDTO for group chat: groupId must be present.");
            throw new IllegalArgumentException("For group chat, groupId must be present.");
        }
        Long groupId = chatRoomDTO.groupId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Remove user failed: Group not found with ID: {}", groupId);
                    return new RuntimeException("Group not found");
                });

        Optional<ChatRoom> existing = chatRoomRepository.findByGroupAndChatroomType(group, ChatRoomType.GROUP);

        if (existing.isPresent()) {
            return chatRoomMapper.chatRoomResponseDTO(existing.get());
        }
        String chatRoomId = "group_" + groupId;
        Set<User> participants = new HashSet<>(group.getUsers());
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatroomId(chatRoomId);
        chatRoom.setChatroomType(ChatRoomType.GROUP);
        chatRoom.setGroup(group);
        chatRoom.setParticipants(participants);
        chatRoom.setLastActivity(Instant.now());
        return chatRoomMapper.chatRoomResponseDTO(chatRoomRepository.save(chatRoom));
    }

    /**
     * Gets a chat room by its ID, if it exists.
     * @param chatRoomId ChatRoom ID
     * @return Optional<ChatRoom>
     */
    public Optional<ChatRoom> getChatRoomById(String chatRoomId) {
        logger.debug("Attempting to get chatroom by ID: {}", chatRoomId);
        return chatRoomRepository.findById(chatRoomId);
    }

    /**
     * Get the list of chatrooms for a particular user ordered by last activity (most recent first)
     * @param userID user ID of the user
     * @return list of chatroom response DTO ordered by last activity
     */
    public List<ChatRoomResponseDTO> getUserChatRooms(Long userID){
        User user = userService.getUserEntityByID(userID);
        return chatRoomRepository.findAllByParticipantsContainingOrderByLastActivityDesc(user)
                .stream()
                .map(chatRoomMapper::chatRoomResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of messages for a chatroom
     * @param chatroomId ID of the chatroom
     * @return list of messages in the chatroom
     */
    public List<MessageResponseDTO> getMessagesFromChatRoom(String chatroomId){
        return messageRepository.findByChatroomChatroomIdAndSoftDeletedFalseOrderByTimestampAsc(chatroomId)
                .stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Adds participant in a group so to keep the user in the chatroom
     * @param chatroomId ID of the chatroom
     * @param userID ID of the user
     */
    @Transactional
    public void addParticipantsInGroup(String chatroomId, Long userID){
        User user = userService.getUserEntityByID(userID);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> {
                    logger.warn("Failed to add participant {}: Chatroom {} not found", userID, chatroomId);
                    return new RuntimeException("Chatroom not found");
                });
        if(chatRoom.getChatroomType().equals(ChatRoomType.GROUP)){
            chatRoom.getParticipants().add(user);
        }
        chatRoomRepository.save(chatRoom);
    }

    /**
     * Removes participant from a group so to not send the message to other users
     * @param chatroomId ID of the chatroom
     * @param userID ID of the user
     */
    @Transactional
    public void removeParticipantsInGroup(String chatroomId, Long userID) {
        User user = userService.getUserEntityByID(userID);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> {
                    logger.warn("Failed to remove participant {}: Chatroom {} not found", userID, chatroomId);
                    return new RuntimeException("Chatroom not found");
                });
        chatRoom.getParticipants().remove(user);
        chatRoomRepository.save(chatRoom);
    }

    /**
     * Updates last activity details and saves in the entity
     * @param chatroomId ID of the chatroom
     */
    @Transactional
    public void updateLastActivity(String chatroomId){
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> {
                    logger.warn("Failed to update last activity: Chatroom {} not found", chatroomId);
                    return new RuntimeException("Chatroom not found");
                });
        chatRoom.setLastActivity(Instant.now());
        chatRoomRepository.save(chatRoom);
    }

    /**
     * Get chatroom IDs where user is a participant
     * @param userId User ID
     * @return List of chatroom IDs
     */
    public List<String> getUserChatroomIds(Long userId) {
        User user = userService.getUserEntityByID(userId);
        return chatRoomRepository.findAllByParticipantsContaining(user)
                .stream()
                .map(ChatRoom::getChatroomId)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a chatroom and all associated messages.
     * @param chatroomId ID of the chatroom to delete
     */
    @Transactional
    public void deleteChatRoom(String chatroomId) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isPresent()) {
            // Delete all messages in the chatroom first
            messageRepository.deleteByChatroomChatroomId(chatroomId);
            // Delete the chatroom
            chatRoomRepository.delete(chatRoomOpt.get());
        } else {
            logger.warn("Chatroom {} not found for deletion", chatroomId);
        }
    }

    /**
     * Clear all messages for a user in a chatroom (used for personal chat "delete")
     * @param chatroomId ID of the chatroom
     * @param userId ID of the user to clear messages for
     */
    @Transactional
    public void clearMessagesForUser(String chatroomId, Long userId) {
        // Add user to hiddenForUsers for all messages in this chatroom
        messageRepository.findByChatroomChatroomIdAndSoftDeletedFalseOrderByTimestampAsc(chatroomId)
                .forEach(message -> {
                    message.getHiddenForUsers().add(userId);
                    messageRepository.save(message);
                });
    }

}
