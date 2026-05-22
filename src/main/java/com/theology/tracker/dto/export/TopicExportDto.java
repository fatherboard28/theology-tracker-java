package com.theology.tracker.dto.export;

import java.time.LocalDateTime;

public record TopicExportDto(
    Long id,
    String title,
    String description,
    String type,
    Long parentTopicId,
    LocalDateTime createdAt
) {}
