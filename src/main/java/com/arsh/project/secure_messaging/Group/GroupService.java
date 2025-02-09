package com.arsh.project.secure_messaging.Group;


import com.arsh.project.secure_messaging.User.User;
import com.arsh.project.secure_messaging.User.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepo userRepo;

    public GroupService(GroupRepository groupRepository, UserRepo userRepo) {
        this.groupRepository = groupRepository;
        this.userRepo = userRepo;
    }

    public Groups getGroupById(Long id){
        return groupRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Group not found"));
    }

    public Groups createGroup(String name, Long createdById){
        User user = userRepo.findById(createdById)
                .orElseThrow(()->new RuntimeException("User not found"));

        Groups groups = new Groups();
        groups.setCreatedBy(user);
        groups.setName(name);
        return groupRepository.save(groups);
    }

    public void addUsers(Long userId, Long groupId){
        User user = userRepo.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        Groups group = groupRepository.findById(groupId)
                .orElseThrow(()->new RuntimeException("Group not found"));

        group.getUsers().add(user);
        groupRepository.save(group);
    }
}
