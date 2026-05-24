package com.theology.tracker.dto;

import java.util.List;

public record NoteFormDto(
    String title,
    String body,
    List<Long> topicIds
) {}
