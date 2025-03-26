package com.ankitarsh.securemessaging.Message;

import com.ankitarsh.securemessaging.ChatRoom.ChatRoom;
import com.ankitarsh.securemessaging.ChatRoom.ChatRoomService;
import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.Group.GroupService;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.User.UserService;
import com.ankitarsh.securemessaging.enums.MessageStatus;
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

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper, ChatRoomService chatRoomService, UserService userService, GroupService groupService) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.chatRoomService = chatRoomService;
        this.userService = userService;
        this.groupService = groupService;
    }

    /**
     * Sends a personal message from one user to another and saves it in the database.
     */
    public MessageResponseDTO sendPersonalMessage(
            MessageDTO messageDTO){
        String chatRoomId = chatRoomService.getOrCreatePersonalChatRoomId(messageDTO.senderId(), messageDTO.receiverId());
        ChatRoom chatRoom = chatRoomService.getChatroomById(chatRoomId);
        User sender = userService.getUserEntityByID(messageDTO.senderId());
        User receiver = userService.getUserEntityByID(messageDTO.receiverId());
        Message message = messageMapper.createPersonalMessage(chatRoom, sender, receiver, messageDTO.content());
        messageRepository.save(message);
        chatRoomService.updateLastActivity(chatRoomId);
        return messageMapper.toResponseDTO(message);
    }

    /**
     * Sends a group message from one user to a group with multiple users and saves it in the database.
     */
    public MessageResponseDTO sendGroupMessage(MessageDTO messageDTO){
        String chatRoomId = chatRoomService.getOrCreateGroupChatRoomId(messageDTO.groupId());
        ChatRoom chatRoom = chatRoomService.getChatroomById(chatRoomId);
        User sender = userService.getUserEntityByID(messageDTO.senderId());
        Group group = groupService.getGroupEntity(messageDTO.groupId());
        Message message = messageMapper.createGroupMessage(chatRoom, sender, group , messageDTO.content());
        messageRepository.save(message);
        chatRoomService.updateLastActivity(chatRoomId);
        return messageMapper.toResponseDTO(message);
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
     *
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
