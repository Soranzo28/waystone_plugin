package dev.soranzo.dto;

import java.util.UUID;

public record WaystoneYamlDTO(
    String stringLocation,
    String name,
    boolean active,
    UUID owner
) {
}
