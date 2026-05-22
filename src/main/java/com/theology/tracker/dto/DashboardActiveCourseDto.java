package com.theology.tracker.dto;

import java.time.LocalDate;

public record DashboardActiveCourseDto(Long id, String title, int progressPct, LocalDate targetDate) {}
