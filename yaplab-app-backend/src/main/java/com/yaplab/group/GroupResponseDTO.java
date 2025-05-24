package com.yaplab.group;

import java.util.List;

public record GroupResponseDTO(
    Long id,
    String name,
    List<String> userNames
) {
}
