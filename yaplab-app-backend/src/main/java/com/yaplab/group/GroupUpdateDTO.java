package com.yaplab.group;

import jakarta.validation.constraints.NotEmpty;

/**
 * A Data Transfer Object (DTO) for updating group information.
 * @param name The new group name
 */
public record GroupUpdateDTO(
        @NotEmpty String name
) {
}
