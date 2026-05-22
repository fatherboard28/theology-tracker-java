package com.theology.tracker.dto.export;

import java.time.LocalDateTime;

public record MethodExportDto(
    Long id,
    String name,
    String description,
    String personalNotes,
    LocalDateTime createdAt
) {}
