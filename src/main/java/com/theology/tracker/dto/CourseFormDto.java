package com.theology.tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record CourseFormDto(
    String title,
    String description,
    String status,
    LocalDate startDate,
    LocalDate targetCompletion,
    List<Long> topicIds
) {}
