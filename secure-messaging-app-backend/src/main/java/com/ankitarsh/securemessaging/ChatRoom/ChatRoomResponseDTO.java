package com.ankitarsh.securemessaging.ChatRoom;

import com.ankitarsh.securemessaging.Group.GroupResponseDTO;
import com.ankitarsh.securemessaging.User.UserResponseDTO;
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
