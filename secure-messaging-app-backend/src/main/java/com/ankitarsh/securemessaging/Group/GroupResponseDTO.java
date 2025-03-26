package com.ankitarsh.securemessaging.Group;

import java.util.List;

public record GroupResponseDTO(
    Long id,
    String name,
    List<String> userNames
) {
}
