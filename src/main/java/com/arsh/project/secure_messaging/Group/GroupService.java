package com.arsh.project.secure_messaging.Group;


import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.User.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service class for handling group-related operations such as finding group, creating, and adding users
 * sending, retrieving, updating status, and soft deleting messages.
 */
@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    /**
     * Finds details of group from the id.
     * @param id  The id of the group.
     */
    public Groups getGroupById(Long id){
        return groupRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Group not found"));
    }

    /**
     * Creates a new group and saves it in the database.
     *
     * @param name        The name of the group.
     * @param createdById The id of the user who created the group.
     * @throws RuntimeException if the user is not found.
     * @return The created group.
     */
    public Groups createGroup(String name, Long createdById){
        User user = userRepository.findById(createdById)
                .orElseThrow(()->new RuntimeException("User not found"));

        Groups groups = new Groups();
        groups.setCreatedBy(user);
        groups.setName(name);
        groups.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(groups);
    }

    /**
     * Adds users to the group and saves it in the database.
     *
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     * @throws RuntimeException  If the user is not found.
     * @throws RuntimeException  If the group is not found.
     */
    //To add a user to the group for implementing group messaging feature
    public void addUsers(Long userId, Long groupId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(()->new RuntimeException("Group not found"));

        group.getUsers().add(user);
        groupRepository.save(group);
    }
}
