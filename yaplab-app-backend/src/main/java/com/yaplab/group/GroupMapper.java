package com.yaplab.group;

import com.yaplab.user.User;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service layer to map group from DTOs to entities and vice versa.
 */
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
