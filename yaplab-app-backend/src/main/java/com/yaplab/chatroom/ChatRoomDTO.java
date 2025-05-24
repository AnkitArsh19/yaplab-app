package com.yaplab.chatroom;

import com.yaplab.enums.ChatRoomType;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRoomDTO(
        @NotEmpty String chatroomId,
        ChatRoomType chatRoomType,
        Long groupId,
        @NotEmpty List<Long> participantIds
        ){
}
