package com.yaplab.group;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param id ID of the group
 * @param name name of the group
 * @param userNames List of users associated with the group
 * @param createdById ID of the user who created the group
 * @param createdByName Name of the user who created the group
 * @param createdAt When the group was created
 * @param profilePictureUrl URL of the group's profile picture
 * @param memberCount Number of members in the group
 */
public record GroupResponseDTO(
    Long id,
    String name,
    List<String> userNames,
    Long createdById,
    String createdByName,
    LocalDateTime createdAt,
    String profilePictureUrl,
    int memberCount
) {
}
