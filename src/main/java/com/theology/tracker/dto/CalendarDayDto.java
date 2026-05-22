package com.theology.tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDayDto(
    LocalDate date,
    int intensityLevel,
    int sessionMinutes,
    int sessionCount,
    List<CalendarSessionDto> sessions,
    List<CalendarEventDto> completedItems,
    List<CalendarEventDto> dueItems,
    boolean today,
    boolean currentMonth
) {}
