package com.yaplab.group;

import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public void updateProfilePicture(Long groupId, MultipartFile file){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg") ||
                        contentType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("Only JPEG, JPG, and PNG files are allowed.");
        }

        long maxSize = 5 * 1024 * 1024; // 5MB

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 5MB.");
        }
        String uploadsDir = "/uploads";
        String fileName = "profile_" + groupId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadsDir, fileName);
        try{
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
        }
        catch (IOException e){
            throw new RuntimeException("Failed to store file", e);
        }
        group.setProfilePictureUrl("/uploads/" + fileName);
        groupRepository.save(group);
    }


}