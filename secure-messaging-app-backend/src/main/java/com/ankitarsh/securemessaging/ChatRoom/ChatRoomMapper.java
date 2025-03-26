package com.ankitarsh.securemessaging.ChatRoom;

import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.Group.GroupResponseDTO;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.User.UserResponseDTO;
import com.ankitarsh.securemessaging.enums.ChatRoomType;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatRoomMapper {
    public ChatRoomResponseDTO chatRoomResponseDTO(ChatRoom chatRoom){
        if (chatRoom == null){
            return null;
        }
        return new ChatRoomResponseDTO(
                chatRoom.getChatroomId(),
                chatRoom.getChatroomType(),
                chatRoom.getParticipants().stream()
                        .map(user -> new UserResponseDTO(
                                user.getId(),
                                user.getUserName(),
                                user.getEmailId(),
                                user.getMobileNumber(),
                                user.getStatus()
                        ))
                        .collect(Collectors.toList()),
                chatRoom.getGroup() != null ? new GroupResponseDTO(
                        chatRoom.getGroup().getId(),
                        chatRoom.getGroup().getName(),
                        chatRoom.getGroup().getUsers().stream()
                                .map(User::getUserName)
                                .collect(Collectors.toList())
                ) : null,
                chatRoom.getLastActivity()
        );
    }

    public ChatRoom toChatRoom(ChatRoomDTO chatRoomDTO, Group group, Set<User> participants){
        if(chatRoomDTO == null){
            return null;
        }
        ChatRoom chatRoom = new ChatRoom(chatRoomDTO.chatroomId(), group, participants);
        chatRoom.setChatroomType(chatRoomDTO.chatRoomType());
        return chatRoom;
    }
}
