package com.theology.tracker.service;

import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarkdownExportService {

    private final NoteRepository noteRepo;
    private final StudySessionRepository sessionRepo;
    private final CourseRepository courseRepo;
    private final UnitRepository unitRepo;
    private final TopicRepository topicRepo;
    private final MethodRepository methodRepo;

    public MarkdownExportService(
        NoteRepository noteRepo,
        StudySessionRepository sessionRepo,
        CourseRepository courseRepo,
        UnitRepository unitRepo,
        TopicRepository topicRepo,
        MethodRepository methodRepo
    ) {
        this.noteRepo = noteRepo;
        this.sessionRepo = sessionRepo;
        this.courseRepo = courseRepo;
        this.unitRepo = unitRepo;
        this.topicRepo = topicRepo;
        this.methodRepo = methodRepo;
    }

    public String buildMarkdownExport() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Theology Study Tracker — Notes Export\n\n");

        appendCourseNotes(sb);
        appendUnitNotes(sb);
        appendTopicNotes(sb);
        appendMethodNotes(sb);
        appendSessionNotes(sb);
        appendSessionReflections(sb);

        return sb.toString();
    }

    private void appendCourseNotes(StringBuilder sb) {
        List<Course> courses = courseRepo.findAll();
        for (Course course : courses) {
            List<Note> notes = noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
                NoteParentType.COURSE, course.getId());
            if (notes.isEmpty()) continue;
            sb.append("## Course: ").append(course.getTitle()).append("\n\n");
            for (Note note : notes) {
                appendNote(sb, note);
            }
        }
    }

    private void appendUnitNotes(StringBuilder sb) {
        List<Unit> units = unitRepo.findAll();
        for (Unit unit : units) {
            List<Note> notes = noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
                NoteParentType.UNIT, unit.getId());
            if (notes.isEmpty()) continue;
            sb.append("## Unit: ").append(unit.getCourse().getTitle())
              .append(" › ").append(unit.getTitle()).append("\n\n");
            for (Note note : notes) {
                appendNote(sb, note);
            }
        }
    }

    private void appendTopicNotes(StringBuilder sb) {
        List<Topic> topics = topicRepo.findAll();
        for (Topic topic : topics) {
            List<Note> notes = noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
                NoteParentType.TOPIC, topic.getId());
            if (notes.isEmpty()) continue;
            sb.append("## Topic: ").append(topic.getTitle()).append("\n\n");
            for (Note note : notes) {
                appendNote(sb, note);
            }
        }
    }

    private void appendMethodNotes(StringBuilder sb) {
        List<Method> methods = methodRepo.findAll();
        for (Method method : methods) {
            List<Note> notes = noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
                NoteParentType.METHOD, method.getId());
            if (notes.isEmpty()) continue;
            sb.append("## Method: ").append(method.getName()).append("\n\n");
            for (Note note : notes) {
                appendNote(sb, note);
            }
        }
    }

    private void appendSessionNotes(StringBuilder sb) {
        List<StudySession> sessions = sessionRepo.findAll();
        boolean headerPrinted = false;
        for (StudySession session : sessions) {
            List<Note> notes = noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
                NoteParentType.SESSION, session.getId());
            if (notes.isEmpty()) continue;
            if (!headerPrinted) {
                sb.append("## Session Notes\n\n");
                headerPrinted = true;
            }
            sb.append("### Session — ").append(session.getSessionDate())
              .append(" (").append(session.getDurationMinutes()).append(" min)\n\n");
            for (Note note : notes) {
                appendNote(sb, note);
            }
        }
    }

    private void appendNote(StringBuilder sb, Note note) {
        sb.append("### ").append(note.getTitle()).append("\n\n");
        sb.append("*Last modified: ").append(note.getUpdatedAt()).append("*\n\n");
        if (note.getBody() != null && !note.getBody().isBlank()) {
            sb.append(note.getBody()).append("\n\n");
        }
        sb.append("---\n\n");
    }

    private void appendSessionReflections(StringBuilder sb) {
        List<StudySession> sessions = sessionRepo.findAll().stream()
            .filter(s -> s.getReflectionNote() != null && !s.getReflectionNote().isBlank())
            .toList();
        if (sessions.isEmpty()) return;

        sb.append("## Session Reflections\n\n");
        for (StudySession session : sessions) {
            sb.append("### ").append(session.getSessionDate())
              .append(" — ").append(session.getDurationMinutes()).append(" min");
            if (session.getWorkItem() != null) {
                sb.append(" (").append(session.getWorkItem().getTitle()).append(")");
            }
            sb.append("\n\n");
            sb.append(session.getReflectionNote()).append("\n\n");
            sb.append("---\n\n");
        }
    }
}
