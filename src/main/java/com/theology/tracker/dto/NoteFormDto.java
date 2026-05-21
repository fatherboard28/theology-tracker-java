package com.theology.tracker.dto;

import java.util.List;

public record NoteFormDto(
    String title,
    String body,
    String parentType,
    Long parentId,
    List<Long> topicIds
) {}
