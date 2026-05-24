package com.theology.tracker.dto;

import java.time.LocalDate;

public record TaskFormDto(
    String title,
    String description,
    String status,
    LocalDate dueDate
) {}
