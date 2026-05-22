package com.theology.tracker.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UnitExportDto(
    Long id,
    Long courseId,
    String title,
    String description,
    int unitOrder,
    LocalDate targetCompletion,
    LocalDate actualCompletion,
    LocalDateTime createdAt
) {}
