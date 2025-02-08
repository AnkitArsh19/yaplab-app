package com.arsh.project.secure_messaging.Group;


import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public Groups getGroupById(Long id){
        return groupRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Group not found"));
    }
}
