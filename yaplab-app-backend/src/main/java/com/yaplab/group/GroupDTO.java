package com.yaplab.group;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GroupDTO(
        Long id,
        @NotEmpty String name,
        @NotEmpty List<Long> userId
) {
}
