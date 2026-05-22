package com.theology.tracker.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SessionExportDto(
    Long id,
    LocalDate sessionDate,
    Integer durationMinutes,
    Long workItemId,
    Long methodId,
    String reflectionNote,
    LocalDateTime createdAt
) {}
