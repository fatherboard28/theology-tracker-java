package com.theology.tracker.dto;

public record CalendarSessionDto(
    Long id,
    int durationMinutes,
    String workItemTitle
) {}
