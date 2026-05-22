package com.theology.tracker.dto;

import java.time.LocalDate;

public record CalendarEventDto(
    Long entityId,
    Long parentId,
    String title,
    String entityType,
    String link,
    LocalDate date,
    boolean overdue
) {}
