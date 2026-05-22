package com.theology.tracker.dto.export;

import java.time.LocalDateTime;
import java.util.List;

public record FullExportDto(
    LocalDateTime exportedAt,
    int version,
    List<MethodExportDto> methods,
    List<TopicExportDto> topics,
    List<CourseExportDto> courses,
    List<UnitExportDto> units,
    List<WorkItemExportDto> workItems,
    List<NoteExportDto> notes,
    List<SessionExportDto> sessions,
    List<ScriptureTagExportDto> scriptureTags,
    List<JoinRecordDto> courseTopics,
    List<JoinRecordDto> unitTopics,
    List<JoinRecordDto> workItemTopics,
    List<JoinRecordDto> noteTopics,
    List<JoinRecordDto> sessionTopics,
    List<JoinRecordDto> noteWorkItems
) {}
