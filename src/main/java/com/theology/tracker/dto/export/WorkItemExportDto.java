package com.theology.tracker.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkItemExportDto(
    Long id,
    String workItemType,
    String title,
    String status,
    Integer estimatedDuration,
    LocalDate dueDate,
    LocalDate completionDate,
    String generalNotes,
    Long unitId,
    Long owningTopicId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    // Reading
    String source,
    String author,
    String location,
    String format,
    // Assignment
    String description,
    // Paper
    String promptOrTopic,
    Integer wordCountTarget,
    String scoreOrGrade,
    // PracticeSessionItem
    Long methodId,
    String scripturePassage,
    Integer durationMinutes
) {}
