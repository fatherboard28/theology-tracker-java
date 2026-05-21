package com.theology.tracker.dto;

import java.time.LocalDate;

public record UnitFormDto(
    String title,
    String description,
    LocalDate targetCompletion
) {}
