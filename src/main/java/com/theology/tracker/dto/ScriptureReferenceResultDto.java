package com.theology.tracker.dto;

import com.theology.tracker.model.StudySession;
import com.theology.tracker.model.WorkItem;

import java.util.List;

public record ScriptureReferenceResultDto(
    String query,
    List<WorkItem> workItems,
    List<StudySession> sessions
) {}
