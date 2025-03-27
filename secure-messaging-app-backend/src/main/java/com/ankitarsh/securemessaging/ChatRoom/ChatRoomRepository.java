package com.ankitarsh.securemessaging.ChatRoom;

import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByParticipantsContainingAndChatroomType(User user, ChatRoomType chatroomType);

    Optional<ChatRoom> findByGroupAndChatroomType(Group group, ChatRoomType chatroomType);

    List<ChatRoom> findAllByParticipantsContaining(User user);
}
