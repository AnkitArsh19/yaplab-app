package com.ankitarsh.securemessaging.ChatRoom;

import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.Group.GroupRepository;
import com.ankitarsh.securemessaging.Group.GroupService;
import com.ankitarsh.securemessaging.Message.MessageResponseDTO;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.User.UserRepository;
import com.ankitarsh.securemessaging.User.UserService;
import com.ankitarsh.securemessaging.enums.ChatRoomType;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {
    private final UserService userService;
    private final GroupService groupService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final UserRepository userRepository;

    public ChatRoomService(UserService userService, GroupService groupService, ChatRoomRepository chatRoomRepository, ChatRoomMapper chatRoomMapper, UserRepository userRepository) {
        this.userService = userService;
        this.groupService = groupService;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMapper = chatRoomMapper;
        this.userRepository = userRepository;
    }

    public String getOrCreatePersonalChatRoomId(
            Long senderID,
            Long receiverID) {
        User sender = userService.getUserEntityByID(senderID);
        User receiver = userService.getUserEntityByID(receiverID);
        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findPersonalChatRoomID(sender, receiver);
        if (existingChatRoom.isEmpty()) {
            existingChatRoom = chatRoomRepository.findPersonalChatRoomID(receiver, sender);  // Check the reversed order
        }
        if (existingChatRoom.isPresent()){
            return existingChatRoom.get().getChatroomId();
        }
        return  sender.getId() < receiver.getId()
                    ? sender.getId() + "_" + receiver.getId()
                    : receiver.getId() + "_" + sender.getId();
    }

    public String getOrCreateGroupChatRoomId(
        Long groupID){
        Group group = groupService.getGroupEntity(groupID);
        return chatRoomRepository.findGroupChatRoomID(group)
                .map(ChatRoom::getChatroomId)
                .orElse("group_" + group.getId());
    }

    public ChatRoom getChatroomById(String chatroomId){
        return chatRoomRepository.findById(chatroomId)
                .orElse(null);
    }


    public ChatRoomResponseDTO createChatRoom(ChatRoomDTO chatRoomDTO) {

        Set<User> participants = new HashSet<>(userRepository.findAllById(chatRoomDTO.participantIds()));
        if (participants.isEmpty()){
            throw new IllegalArgumentException("No valid participants found");
        }
        Group group = null;
        if (chatRoomDTO.chatRoomType() == ChatRoomType.GROUP){
            if (chatRoomDTO.groupId()==null){
                throw new IllegalArgumentException("Group Id is required for group chats");
            }
            group = groupService.getGroupEntity(chatRoomDTO.groupId());
            String chatroomId = getOrCreateGroupChatRoomId(chatRoomDTO.groupId());
            chatRoomDTO = new ChatRoomDTO(chatroomId, chatRoomDTO.chatRoomType(), chatRoomDTO.groupId(), chatRoomDTO.participantIds());
            participants.addAll(group.getUsers());
        }
        else if (chatRoomDTO.chatRoomType() == ChatRoomType.PERSONAL){
            if (participants.size() != 2){
                throw new IllegalArgumentException("Personal chat must have exactly two participants");
            }
            List<User> participantList = new ArrayList<>(participants);
            User user1 = participantList.get(0);
            User user2 = participantList.get(1);

            String chatroomId = getOrCreatePersonalChatRoomId(user1.getId(), user2.getId());
            chatRoomDTO = new ChatRoomDTO(chatroomId, chatRoomDTO.chatRoomType(), chatRoomDTO.groupId(), chatRoomDTO.participantIds());

        }
        ChatRoom chatRoom = chatRoomMapper.toChatRoom(chatRoomDTO, group, participants);
        if (chatRoomDTO.chatRoomType()==ChatRoomType.GROUP){
            chatRoom.setGroup(group);
        }
        return chatRoomMapper.chatRoomResponseDTO(chatRoomRepository.save(chatRoom));

    }

    public List<ChatRoomResponseDTO> getUserChatRooms(Long userID){
        User user = userService.getUserEntityByID(userID);
        return chatRoomRepository.findAllByParticipantsContaining(user)
                .stream()
                .map(chatRoomMapper::chatRoomResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getMessagesFromChatRoom(String chatroomId){
        return chatRoomRepository.findById(chatroomId)
                .map(chatRoom -> chatRoom.getMessages().stream()
                    .map(message -> new MessageResponseDTO(
                            message.getId(),
                            message.getSender().getUserName(),
                            message.getContent(),
                            message.getTimestamp(),
                            message.getMessageStatus()
                )).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

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

    public void updateLastActivity(String chatroomId){
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found"));
        chatRoom.setLastActivity(LocalDateTime.now());
        chatRoomMapper.chatRoomResponseDTO(chatRoomRepository.save(chatRoom));
    }

}

