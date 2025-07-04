package com.yaplab.group;

import com.yaplab.chatroom.ChatRoomService;
import com.yaplab.user.User;
import com.yaplab.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    private final SimpMessagingTemplate messagingTemplate;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupMapper groupMapper, ChatRoomService chatRoomService, SimpMessagingTemplate messagingTemplate){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMapper = groupMapper;
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Finds details of group from the id.
     * @param id  The id of the group.
     */
    public Group getGroupEntity(Long id){
        return groupRepository.findById(id)
                .orElseThrow(()-> {
                    logger.warn("Group not found with ID: {}", id);
                    return new RuntimeException("Group not found");
                });
    }

    /**
     * Creates a new group and saves it in the database.
     * @param createdById The id of the user who created the group.
     * @return The created group.
     */
    @Transactional
    public GroupResponseDTO createGroup(GroupDTO groupDTO, Long createdById){
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
        return groupMapper.toGroupResponseDTO(savedGroup);
    }

    /**
     * Adds users to the group and saves it in the database.
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     */
    @Transactional
    public void addUsers(Long userId, Long groupId){
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
        chatRoomService.addParticipantsInGroup(chatRoomId, userId);

        broadcastGroupMembershipChange(groupId, userId, user.getUserName(), "USER_JOINED");
    }

    /**
     * Removes users from the group and updates the database.
     * @param userId   The user being added.
     * @param groupId  The group to add the user in.
     */
    @Transactional
    public void removeUser(Long userId, Long groupId) {
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
        chatRoomService.removeParticipantsInGroup(chatRoomId, userId);

        broadcastGroupMembershipChange(groupId, userId, user.getUserName(), "USER_LEFT");
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
    public void updateProfilePicture(Long groupId, MultipartFile file) {
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
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        long maxSize = 5 * 1024 * 1024; // 5MB

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 5MB.");
        }

        // Create absolute path for uploads/groups directory
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path uploadsDir = projectRoot.resolve("uploads").resolve("groups");
        String fileName = "profile_" + groupId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadsDir.resolve(fileName);

        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            logger.error("Failed to store profile picture for group {}: {}", groupId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
        String profilePictureUrl = "/files/serve/groups/" + fileName;
        group.setProfilePictureUrl(profilePictureUrl);
        Group savedGroup = groupRepository.save(group);
        
        // Broadcast group update to all members
        broadcastGroupUpdate(groupId, savedGroup);
    }

    /**
     * Gets group details with full information including creator and members.
     * @param groupId The ID of the group.
     * @return GroupResponseDTO with complete group information.
     */
    public GroupResponseDTO getGroupDetails(Long groupId) {
        Group group = getGroupEntity(groupId);
        return groupMapper.toGroupResponseDTO(group);
    }

    /**
     * Deletes a group. Only the creator can delete the group.
     * @param groupId The ID of the group to delete.
     * @param userId The ID of the user attempting to delete.
     */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = getGroupEntity(groupId);
        
        // Check if the user is the creator
        if (!group.getCreatedBy().getId().equals(userId)) {
            logger.warn("Delete group failed: User {} is not the creator of group {}", userId, groupId);
            throw new RuntimeException("Only the group creator can delete the group");
        }
        
        // Remove all users from the group first (this will also update chatroom participants)
        group.getUsers().clear();
        groupRepository.save(group);
        
        // Delete the chatroom associated with this group
        String chatRoomId = "group_" + groupId;
        chatRoomService.deleteChatRoom(chatRoomId);
        
        // Delete the group
        groupRepository.delete(group);
    }

    /**
     * Updates the group name. Only the creator can update group details.
     * @param groupId The ID of the group to update.
     * @param groupUpdateDTO The new group details.
     * @param userId The ID of the user attempting to update.
     * @return Updated GroupResponseDTO.
     */
    @Transactional
    public GroupResponseDTO updateGroupName(Long groupId, GroupUpdateDTO groupUpdateDTO, Long userId) {
        Group group = getGroupEntity(groupId);
        
        // Check if the user is the creator
        if (!group.getCreatedBy().getId().equals(userId)) {
            logger.warn("Update group failed: User {} is not the creator of group {}", userId, groupId);
            throw new RuntimeException("Only the group creator can update the group");
        }
        
        group.setName(groupUpdateDTO.name());
        Group savedGroup = groupRepository.save(group);
        
        // Broadcast group update to all members
        broadcastGroupUpdate(groupId, savedGroup);
        
        return groupMapper.toGroupResponseDTO(savedGroup);
    }

    /**
     * Broadcasts group membership change events to all group members via WebSocket.
     * @param groupId The ID of the group where membership changed.
     * @param userId The ID of the user who joined or left.
     * @param username The username of the user who joined or left.
     * @param eventType The type of event ("USER_JOINED" or "USER_LEFT").
     */
    private void broadcastGroupMembershipChange(Long groupId, Long userId, String username, String eventType) {
        try {
            Map<String, Object> eventData = Map.of(
                "type", eventType,
                "groupId", groupId,
                "userId", userId,
                "username", username,
                "timestamp", System.currentTimeMillis()
            );

            // Broadcast to all users in the group chat room
            String chatRoomDestination = "/topic/group/" + groupId;
            messagingTemplate.convertAndSend(chatRoomDestination, eventData);

        } catch (Exception e) {
            logger.error("Failed to broadcast group membership change: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcasts group update events to all group members via WebSocket.
     * @param groupId The ID of the group that was updated.
     * @param group The updated group entity.
     */
    private void broadcastGroupUpdate(Long groupId, Group group) {
        try {
            Map<String, Object> eventData = Map.of(
                "type", "GROUP_UPDATED",
                "groupId", groupId,
                "data", Map.of(
                    "id", group.getId(),
                    "name", group.getName(),
                    "profilePictureUrl", group.getProfilePictureUrl() != null ? group.getProfilePictureUrl() : ""
                ),
                "timestamp", System.currentTimeMillis()
            );

            // Broadcast to all users in the group chat room
            String chatRoomDestination = "/topic/group/" + groupId;
            messagingTemplate.convertAndSend(chatRoomDestination, eventData);
            
            logger.info("Broadcasted group update for group {} to {}", groupId, chatRoomDestination);

        } catch (Exception e) {
            logger.error("Failed to broadcast group update: {}", e.getMessage(), e);
        }
    }
}