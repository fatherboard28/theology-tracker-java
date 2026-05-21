package com.theology.tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record UnitFormDto(
    String title,
    String description,
    LocalDate targetCompletion,
    List<Long> topicIds
) {}
