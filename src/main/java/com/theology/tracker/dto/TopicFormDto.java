package com.theology.tracker.dto;

public record TopicFormDto(
    String title,
    String description,
    Long parentTopicId
) {}
