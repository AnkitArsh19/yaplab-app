package com.yaplab.group;

import com.yaplab.chatroom.ChatRoomService;
import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
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

    /**
     * Logger for GroupService
     * This logger is used to log various events and errors in the GroupService class.
     * It helps in debugging and tracking the flow of operations related to group management.
     */
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    /**
     * Constructor based dependency injection
     */
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;
    private final ChatRoomService chatRoomService;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupMapper groupMapper, ChatRoomService chatRoomService){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMapper = groupMapper;
        this.chatRoomService = chatRoomService;
    }

    /**
     * Finds details of group from the id.
     * @param id  The id of the group.
     */
    public Group getGroupEntity(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(()-> {
                    logger.warn("Group not found with ID: {}", id);
                    return new RuntimeException("Group not found");
                });
        logger.info("Group found with ID: {}", id);
        return group;
    }

    /**
     * Creates a new group and saves it in the database.
     * @param createdById The id of the user who created the group.
     * @return The created group.
     */
    @Transactional
    public GroupResponseDTO createGroup(GroupDTO groupDTO, Long createdById){
        logger.info("Attempting to create group: {}", groupDTO.name());
        User creator = userRepository.findById(createdById)
                .orElseThrow(()->{
                    logger.error("Group creation failed: Creator user not found with ID: {}", createdById);
                    return new RuntimeException("User not found with the id: " + createdById);
                });

        List<User> users = userRepository.findAllById(groupDTO.userId());
        if (users.size() != groupDTO.userId().size()) {
            logger.warn("Group creation: Not all specified users found for group {}. Found {} out of {}", groupDTO.name(), users.size(), groupDTO.userId().size());
        }
        if (users.isEmpty()){
            throw new IllegalArgumentException("User not found with the given id");
        }
        Group group = new Group();
        group.setCreatedBy(creator);
        group.setName(groupDTO.name());
        group.setCreatedAt(LocalDateTime.now());
        group.setUsers(new HashSet<>(users));
        Group savedGroup = groupRepository.save(group);
        logger.info("Group created successfully with ID: {} and name: {}", savedGroup.getId(), savedGroup.getName());
        return groupMapper.toGroupResponseDTO(savedGroup);
    }

    /**
     * Adds users to the group and saves it in the database.
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     */
    @Transactional
    public void addUsers(Long userId, Long groupId){
        logger.info("Attempting to add user {} to group {}", userId, groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(()->{
                    logger.warn("Add user failed: User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->{
                    logger.warn("Add user failed: Group not found with ID: {}", groupId);
                    return new RuntimeException("Group not found");
                });

        group.getUsers().add(user);
        groupRepository.save(group);

        String chatRoomId = "group_" + groupId;
        chatRoomService.addParticipantsInGroup(chatRoomId, groupId);
        logger.info("User {} added to group {} successfully.", userId, groupId);
    }

    /**
     * Removes users from the group and updates the database.
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     */
    @Transactional
    public void removeUser(Long userId, Long groupId) {
        logger.info("Attempting to remove user {} from group {}", userId, groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Remove user failed: User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Remove user failed: Group not found with ID: {}", groupId);
                    return new RuntimeException("Group not found");
                });

        group.getUsers().remove(user);
        groupRepository.save(group);

        String chatRoomId = "group_" + groupId;
        chatRoomService.removeParticipantsInGroup(chatRoomId, groupId);
        logger.info("User {} removed from group {} successfully.", userId, groupId);
    }

    /**
     * Updates the profile picture url with the file stored in the "uploads" folder.
     * Checks that the file size does not exceed 5MB and are of type png/jpeg/jpg.
     * Maintains consistent naming of all pictures.
     * Tries to create a parent directory if it doesn't exist
     * Uploads the file to the directory.
     * @param groupId The group whose picture is being updated
     * @param file The file uploaded by the user.
     */
    @Transactional
    public void updateProfilePicture(Long groupId, MultipartFile file){
        logger.info("Attempting to update profile picture for group {}", groupId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Profile picture update failed: Group not found with ID: {}", groupId);
                    return new IllegalArgumentException("Group not found");
                });
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg") ||
                        contentType.equalsIgnoreCase("image/png"))) {
            logger.warn("Profile picture update failed: Invalid file type for group {}", groupId);
            throw new IllegalArgumentException("Only JPEG, JPG, and PNG files are allowed.");
        }
        if (file.isEmpty()) {
            logger.warn("Profile picture update failed: Uploaded file is empty for group {}", groupId);
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