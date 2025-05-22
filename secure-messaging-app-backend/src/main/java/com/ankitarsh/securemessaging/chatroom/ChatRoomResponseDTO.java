package com.ankitarsh.securemessaging.chatroom;

import com.ankitarsh.securemessaging.group.GroupResponseDTO;
import com.ankitarsh.securemessaging.user.UserResponseDTO;
import com.ankitarsh.securemessaging.enums.ChatRoomType;
import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomResponseDTO(
        String chatroomId,
        ChatRoomType chatRoomType,
        List<UserResponseDTO> participants,
        GroupResponseDTO group,
        LocalDateTime lastActivity
) {
}
