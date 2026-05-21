package com.theology.tracker.dto;

import java.time.LocalDate;

public record CourseFormDto(
    String title,
    String description,
    String status,
    LocalDate startDate,
    LocalDate targetCompletion
) {}
