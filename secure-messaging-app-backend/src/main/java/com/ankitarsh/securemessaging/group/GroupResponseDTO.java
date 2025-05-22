package com.ankitarsh.securemessaging.group;

import java.util.List;

public record GroupResponseDTO(
    Long id,
    String name,
    List<String> userNames
) {
}
