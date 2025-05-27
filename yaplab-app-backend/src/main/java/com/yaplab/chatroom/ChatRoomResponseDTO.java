package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import com.yaplab.group.GroupResponseDTO;
import com.yaplab.user.UserResponseDTO;

import java.time.Instant;
import java.util.List;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param chatroomId The unique ID of the chatroom
 * @param chatRoomType Type of the chatroom. Personal or group
 * @param participants List of participants with encapsulated in another response dto
 * @param group Group if exists also passed as dto
 * @param lastActivity last activity of the chatroom
 */
public record ChatRoomResponseDTO(
        String chatroomId,
        ChatRoomType chatRoomType,
        List<UserResponseDTO> participants,
        GroupResponseDTO group,
        Instant lastActivity
) {
}
