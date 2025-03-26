package com.ankitarsh.securemessaging.Group;


import com.ankitarsh.securemessaging.User.User;
import com.ankitarsh.securemessaging.User.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Service class for handling group-related operations such as finding group, creating, and adding users
 * sending, retrieving, updating status, and soft deleting messages.
 */
@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupMapper groupMapper) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMapper = groupMapper;
    }

    /**
     * Finds details of group from the id.
     * @param id  The id of the group.
     */
    public GroupResponseDTO getGroupById(Long id){
        return groupMapper.toGroupResponseDTO(groupRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Group not found")));
    }

    public Group getGroupEntity(Long id){
        return groupRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Group not found"));
    }

    /**
     * Creates a new group and saves it in the database.
     * @param createdById The id of the user who created the group.
     * @throws RuntimeException if the user is not found.
     * @return The created group.
     */
    public GroupResponseDTO createGroup(GroupDTO groupDTO, Long createdById){
        User creator = userRepository.findById(createdById)
                .orElseThrow(()->new RuntimeException("User not found with the id: " + createdById));

        List<User> users = userRepository.findAllById(groupDTO.userId());
        if (users.isEmpty()){
            throw new IllegalArgumentException("User not found with the given id");
        }
        Group group = new Group();
        group.setCreatedBy(creator);
        group.setName(groupDTO.name());
        group.setCreatedAt(LocalDateTime.now());
        group.setUsers(new HashSet<>(users));
        return groupMapper.toGroupResponseDTO(groupRepository.save(group));
    }

    /**
     * Adds users to the group and saves it in the database.
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     * @throws RuntimeException  If the user is not found.
     * @throws RuntimeException  If the group is not found.
     */
    public void addUsers(Long userId, Long groupId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->new RuntimeException("Group not found"));

        group.getUsers().add(user);
        groupRepository.save(group);
    }


}