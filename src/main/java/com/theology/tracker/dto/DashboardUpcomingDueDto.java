package com.theology.tracker.dto;

import java.time.LocalDate;

public record DashboardUpcomingDueDto(String title, LocalDate dueDate, String entityType, String link) {}
