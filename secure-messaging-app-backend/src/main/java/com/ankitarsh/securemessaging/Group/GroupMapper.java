package com.ankitarsh.securemessaging.Group;

import com.ankitarsh.securemessaging.User.User;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class GroupMapper {

    public GroupResponseDTO toGroupResponseDTO(Group group) {
        if (group == null) {
            return null;
        } else {
            return new GroupResponseDTO(
                    group.getId(),
                    group.getName(),
                    group.getUsers().stream()
                            .map(User::getUserName)
                            .collect(Collectors.toList())
            );
        }
    }
}
