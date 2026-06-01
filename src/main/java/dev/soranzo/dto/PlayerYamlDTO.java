package dev.soranzo.dto;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public record PlayerYamlDTO(
        UUID playerUUID,
        HashMap<String, String> connections,
        List<String> discoveries
) {
}
