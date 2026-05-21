package com.theology.tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record WorkItemFormDto(
    String title,
    String type,
    String status,
    Integer estimatedDuration,
    LocalDate dueDate,
    String generalNotes,
    Long unitId,
    Long owningTopicId,
    List<Long> topicIds,
    List<String> scriptureTags,

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

    // Practice Session
    Long methodId,
    String scripturePassage,
    Integer durationMinutes
) {}
