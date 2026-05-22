package com.theology.tracker.dto;

import java.time.LocalDate;

public record HeatmapDayDto(LocalDate date, int intensityLevel, boolean hasDueDate) {}
