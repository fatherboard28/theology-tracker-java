package com.theology.tracker.dto;

import com.theology.tracker.model.Note;
import com.theology.tracker.model.Paper;
import com.theology.tracker.model.Topic;

import java.util.List;

public record SearchResultDto(
    String query,
    List<Topic> topics,
    List<Note> notes,
    List<Paper> papers
) {
    public boolean hasResults() {
        return !topics.isEmpty() || !notes.isEmpty() || !papers.isEmpty();
    }

    public int totalCount() {
        return topics.size() + notes.size() + papers.size();
    }
}
