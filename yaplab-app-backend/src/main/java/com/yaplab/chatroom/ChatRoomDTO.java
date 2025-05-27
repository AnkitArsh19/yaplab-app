package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 A Data Transfer Object (DTO) for chatrooms.
 * This DTO is used to encapsulate the data required for a chatroom.
 * @param chatroomId unique ID for chatroom.
 * @param chatRoomType type of the chatroom. Personal or group
 * @param groupId Group ID if exists
 * @param participantIds List of participants in the chatroom
 */
public record ChatRoomDTO(
        @NotEmpty String chatroomId,
        ChatRoomType chatRoomType,
        Long groupId,
        @NotEmpty List<Long> participantIds
        ){
}
