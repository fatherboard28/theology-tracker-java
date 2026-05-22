package com.theology.tracker.dto.export;

import java.time.LocalDateTime;

public record NoteExportDto(
    Long id,
    String title,
    String body,
    String primaryParentType,
    Long primaryParentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
