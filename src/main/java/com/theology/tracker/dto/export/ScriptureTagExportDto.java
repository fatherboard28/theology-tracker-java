package com.theology.tracker.dto.export;

public record ScriptureTagExportDto(
    Long id,
    String reference,
    String entityType,
    Long entityId
) {}
