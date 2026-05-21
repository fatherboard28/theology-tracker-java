package com.theology.tracker.dto;

import java.time.LocalDate;
import java.util.List;

public record StudySessionFormDto(
    LocalDate sessionDate,
    Integer durationMinutes,
    Long workItemId,
    Long methodId,
    String reflectionNote,
    List<Long> topicIds,
    List<String> scriptureTags
) {}
