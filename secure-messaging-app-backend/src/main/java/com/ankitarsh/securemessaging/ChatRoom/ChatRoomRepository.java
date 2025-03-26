package com.ankitarsh.securemessaging.ChatRoom;

import com.ankitarsh.securemessaging.Group.Group;
import com.ankitarsh.securemessaging.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findPersonalChatRoomID(User sender, User receiver);

    Optional<ChatRoom> findGroupChatRoomID(Group group);

    List<ChatRoom> findAllByParticipantsContaining(User user);
}
