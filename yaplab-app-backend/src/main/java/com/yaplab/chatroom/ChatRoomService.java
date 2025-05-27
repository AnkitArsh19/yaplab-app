package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.Group;
import com.yaplab.group.GroupService;
import com.yaplab.message.Message;
import com.yaplab.message.MessageMapper;
import com.yaplab.message.MessageRepository;
import com.yaplab.message.MessageResponseDTO;
import com.yaplab.user.User;
import com.yaplab.user.UserService;
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
     * Constructor based dependency injection
     */
    private final UserService userService;
    private final GroupService groupService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final MessageMapper messageMapper;
    private final MessageRepository messageRepository; // Ensure this is declared and injected

    public ChatRoomService(UserService userService, GroupService groupService, ChatRoomRepository chatRoomRepository, ChatRoomMapper chatRoomMapper, MessageMapper messageMapper, MessageRepository messageRepository) {
        this.userService = userService;
        this.groupService = groupService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMapper = chatRoomMapper;
        this.messageMapper = messageMapper;
        this.messageRepository = messageRepository; // Ensure this is initialized
    }

    /**
     * Finds or creates a personal chat room between two users based on ChatRoomDTO.
     * Uses repository method to find existing personal chat.
     * Creates a new one if the chatroom doesn't exist
     * @param chatRoomDTO DTO containing participant IDs.
     * @return ChatRoomResponseDTO
     */
    @Transactional
    public ChatRoomResponseDTO getOrCreatePersonalChatRoom(ChatRoomDTO chatRoomDTO) {
        if (chatRoomDTO.participantIds() == null || chatRoomDTO.participantIds().size() != 2) {
            throw new IllegalArgumentException("For personal chat, participantIds must contain exactly two user IDs.");
        }
        Long userId1 = chatRoomDTO.participantIds().get(0);
        Long userId2 = chatRoomDTO.participantIds().get(1);

        User user1 = userService.getUserEntityByID(userId1);
        User user2 = userService.getUserEntityByID(userId2);


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
            throw new IllegalArgumentException("For group chat, groupId must be present.");
        }
        Long groupId = chatRoomDTO.groupId();
        Group group = groupService.getGroupEntity(groupId);
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
        return chatRoomRepository.findById(chatRoomId);
    }

    /**
     * Get the list of chatrooms for a particular user
     * @param userID user ID of the user
     * @return list of chatroom response DTO
     */
    public List<ChatRoomResponseDTO> getUserChatRooms(Long userID){
        User user = userService.getUserEntityByID(userID);
        return chatRoomRepository.findAllByParticipantsContaining(user)
                .stream()
                .map(chatRoomMapper::chatRoomResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of messages for a chatroom
     * @param chatroomId ID of the chatroom
     * @return list of messages in the chatroom
     */
    /**
     * Retrieves all non-soft-deleted messages for a given chatroom.
     * @param chatroomId The ID of the chatroom.
     * @return A list of MessageResponseDTOs for non-soft-deleted messages.
     */
    public List<MessageResponseDTO> getMessagesFromChatRoom(String chatroomId){
        // Use the repository method to find messages where softDeleted is false
        List<Message> messages = messageRepository.findByChatroom_ChatroomIdAndSoftDeletedFalse(chatroomId);

        return messages.stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Adds participant in a group so to keep the user in the chatroom
     * @param chatroomId ID of the chatroom
     * @param userID ID of the user
     * @return ChatroomResponseDTO with updated details
     */
    @Transactional
    public ChatRoomResponseDTO addParticipantsInGroup(String chatroomId, Long userID){
        User user = userService.getUserEntityByID(userID);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found"));
        if(chatRoom.getChatroomType().equals(ChatRoomType.GROUP) && !chatRoom.getParticipants().contains(user)){
            chatRoom.getParticipants().add(user);
            chatRoomRepository.save(chatRoom);
        }
        return chatRoomMapper.chatRoomResponseDTO(chatRoom);
    }

    /**
     * Removes participant from a group so to not send the message to other users
     * @param chatroomId ID of the chatroom
     * @param userID ID of the user
     * @return ChatroomResponseDTO with updated details
     */
    @Transactional
    public ChatRoomResponseDTO removeParticipantsInGroup(String chatroomId, Long userID){
        User user = userService.getUserEntityByID(userID);
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found"));
        if(chatRoom.getParticipants().contains(user)){
            chatRoom.getParticipants().remove(user);
            chatRoomRepository.save(chatRoom);
        }
        return chatRoomMapper.chatRoomResponseDTO(chatRoom);
    }

    /**
     * Updates last activity details and saves in the entity
     * @param chatroomId ID of the chatroom
     */
    @Transactional
    public void updateLastActivity(String chatroomId){
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found"));
        chatRoom.setLastActivity(Instant.now());
        chatRoomRepository.save(chatRoom);
    }
}
