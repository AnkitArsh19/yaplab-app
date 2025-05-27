package com.yaplab.group;

import java.util.List;

/**
 * A Response DTO to send the response from the server to the client.
 * Only sends required information by not exposing the whole Entity.
 * @param id ID of the group
 * @param name name of the group
 * @param userNames List of users associated with the group
 */
public record GroupResponseDTO(
    Long id,
    String name,
    List<String> userNames
) {
}
