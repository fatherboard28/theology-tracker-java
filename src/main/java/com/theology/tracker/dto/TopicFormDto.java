package com.theology.tracker.dto;

public record TopicFormDto(
    String title,
    String description,
    String type,
    Long parentTopicId
) {}
