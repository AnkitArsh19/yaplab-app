package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.GroupResponseDTO;
import com.yaplab.user.UserResponseDTO;

import java.time.Instant;
import java.util.List;

public record ChatRoomResponseDTO(
        String chatroomId,
        ChatRoomType chatRoomType,
        List<UserResponseDTO> participants,
        GroupResponseDTO group,
        Instant lastActivity
) {
}
