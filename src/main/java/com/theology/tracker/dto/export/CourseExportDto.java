package com.theology.tracker.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CourseExportDto(
    Long id,
    String title,
    String description,
    String status,
    LocalDate startDate,
    LocalDate targetCompletion,
    LocalDate actualCompletion,
    LocalDateTime createdAt
) {}
