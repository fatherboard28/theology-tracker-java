package com.theology.tracker.dto;

import java.util.List;

public record PaperFormDto(
    String title,
    String thesis,
    String author,
    String status,
    List<Long> topicIds
) {}
