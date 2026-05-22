package com.theology.tracker.dto;

import com.theology.tracker.model.Course;
import com.theology.tracker.model.Note;
import com.theology.tracker.model.StudySession;
import com.theology.tracker.model.Topic;
import com.theology.tracker.model.Unit;
import com.theology.tracker.model.WorkItem;

import java.util.List;

public record SearchResultDto(
    String query,
    List<Course> courses,
    List<Unit> units,
    List<Topic> topics,
    List<WorkItem> workItems,
    List<Note> notes,
    List<StudySession> sessions
) {
    public boolean hasResults() {
        return !courses.isEmpty() || !units.isEmpty() || !topics.isEmpty()
            || !workItems.isEmpty() || !notes.isEmpty() || !sessions.isEmpty();
    }

    public int totalCount() {
        return courses.size() + units.size() + topics.size()
            + workItems.size() + notes.size() + sessions.size();
    }
}
