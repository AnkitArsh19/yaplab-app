package com.yaplab.group;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A Data Transfer Object (DTO) for group information.
 * This DTO is used to encapsulate the data required.
 * @param id ID of the group
 * @param name The group name
 * @param userId List of ID's of users in the group
 */
public record GroupDTO(
        Long id,
        @NotEmpty String name,
        @NotEmpty List<Long> userId
) {
}
