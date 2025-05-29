package com.yaplab.chatroom;

import com.yaplab.group.GroupResponseDTO;
import com.yaplab.user.User;
import com.yaplab.user.UserResponseDTO;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service layer to map chatroom from DTOs to entities and vice versa.
 */
@Service
public class ChatRoomMapper {
    /**
     * Returns a chatroomResponseDTO from chatroom object
     * @param chatRoom the chatroom object
     */
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
                                user.getStatus(),
                                user.getProfilePictureUrl()
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
}
