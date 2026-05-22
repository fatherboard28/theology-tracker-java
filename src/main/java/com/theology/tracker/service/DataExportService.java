package com.theology.tracker.service;

import com.theology.tracker.dto.export.*;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataExportService {

    private final CourseRepository courseRepo;
    private final UnitRepository unitRepo;
    private final TopicRepository topicRepo;
    private final WorkItemRepository workItemRepo;
    private final NoteRepository noteRepo;
    private final StudySessionRepository sessionRepo;
    private final MethodRepository methodRepo;
    private final ScriptureTagRepository scriptureTagRepo;

    public DataExportService(
        CourseRepository courseRepo,
        UnitRepository unitRepo,
        TopicRepository topicRepo,
        WorkItemRepository workItemRepo,
        NoteRepository noteRepo,
        StudySessionRepository sessionRepo,
        MethodRepository methodRepo,
        ScriptureTagRepository scriptureTagRepo
    ) {
        this.courseRepo = courseRepo;
        this.unitRepo = unitRepo;
        this.topicRepo = topicRepo;
        this.workItemRepo = workItemRepo;
        this.noteRepo = noteRepo;
        this.sessionRepo = sessionRepo;
        this.methodRepo = methodRepo;
        this.scriptureTagRepo = scriptureTagRepo;
    }

    public FullExportDto buildExport() {
        List<MethodExportDto> methods = methodRepo.findAll().stream()
            .map(m -> new MethodExportDto(m.getId(), m.getName(), m.getDescription(), m.getPersonalNotes(), m.getCreatedAt()))
            .toList();

        List<TopicExportDto> topics = topicRepo.findAll().stream()
            .map(t -> new TopicExportDto(
                t.getId(), t.getTitle(), t.getDescription(), t.getType().name(),
                t.getParentTopic() != null ? t.getParentTopic().getId() : null,
                t.getCreatedAt()))
            .toList();

        List<CourseExportDto> courses = courseRepo.findAll().stream()
            .map(c -> new CourseExportDto(
                c.getId(), c.getTitle(), c.getDescription(), c.getStatus().name(),
                c.getStartDate(), c.getTargetCompletion(), c.getActualCompletion(), c.getCreatedAt()))
            .toList();

        List<UnitExportDto> units = unitRepo.findAll().stream()
            .map(u -> new UnitExportDto(
                u.getId(), u.getCourse().getId(), u.getTitle(), u.getDescription(),
                u.getUnitOrder(), u.getTargetCompletion(), u.getActualCompletion(), u.getCreatedAt()))
            .toList();

        List<WorkItemExportDto> workItems = workItemRepo.findAll().stream()
            .map(this::mapWorkItem)
            .toList();

        List<NoteExportDto> notes = noteRepo.findAll().stream()
            .map(n -> new NoteExportDto(
                n.getId(), n.getTitle(), n.getBody(),
                n.getPrimaryParentType().name(), n.getPrimaryParentId(),
                n.getCreatedAt(), n.getUpdatedAt()))
            .toList();

        List<SessionExportDto> sessions = sessionRepo.findAll().stream()
            .map(s -> new SessionExportDto(
                s.getId(), s.getSessionDate(), s.getDurationMinutes(),
                s.getWorkItem() != null ? s.getWorkItem().getId() : null,
                s.getMethod() != null ? s.getMethod().getId() : null,
                s.getReflectionNote(), s.getCreatedAt()))
            .toList();

        List<ScriptureTagExportDto> scriptureTags = scriptureTagRepo.findAll().stream()
            .map(st -> new ScriptureTagExportDto(st.getId(), st.getReference(), st.getEntityType().name(), st.getEntityId()))
            .toList();

        List<JoinRecordDto> courseTopics = new ArrayList<>();
        courseRepo.findAll().forEach(c ->
            c.getTopics().forEach(t -> courseTopics.add(new JoinRecordDto(c.getId(), t.getId()))));

        List<JoinRecordDto> unitTopics = new ArrayList<>();
        unitRepo.findAll().forEach(u ->
            u.getTopics().forEach(t -> unitTopics.add(new JoinRecordDto(u.getId(), t.getId()))));

        List<JoinRecordDto> workItemTopics = new ArrayList<>();
        workItemRepo.findAll().forEach(w ->
            w.getTopicTags().forEach(t -> workItemTopics.add(new JoinRecordDto(w.getId(), t.getId()))));

        List<JoinRecordDto> noteTopics = new ArrayList<>();
        noteRepo.findAll().forEach(n ->
            n.getTopicTags().forEach(t -> noteTopics.add(new JoinRecordDto(n.getId(), t.getId()))));

        List<JoinRecordDto> sessionTopics = new ArrayList<>();
        sessionRepo.findAll().forEach(s ->
            s.getTopics().forEach(t -> sessionTopics.add(new JoinRecordDto(s.getId(), t.getId()))));

        List<JoinRecordDto> noteWorkItems = new ArrayList<>();
        noteRepo.findAll().forEach(n ->
            n.getWorkItemRefs().forEach(w -> noteWorkItems.add(new JoinRecordDto(n.getId(), w.getId()))));

        return new FullExportDto(
            LocalDateTime.now(), 1,
            methods, topics, courses, units, workItems, notes, sessions, scriptureTags,
            courseTopics, unitTopics, workItemTopics, noteTopics, sessionTopics, noteWorkItems
        );
    }

    private WorkItemExportDto mapWorkItem(WorkItem w) {
        String type = w.getTypeLabel().toUpperCase().replace(" ", "_");
        Long unitId = w.getUnit() != null ? w.getUnit().getId() : null;
        Long owningTopicId = w.getOwningTopic() != null ? w.getOwningTopic().getId() : null;

        String source = null, author = null, location = null, format = null;
        String description = null;
        String promptOrTopic = null; Integer wordCountTarget = null; String scoreOrGrade = null;
        Long methodId = null; String scripturePassage = null; Integer durationMinutes = null;

        if (w instanceof Reading r) {
            type = "READING";
            source = r.getSource();
            author = r.getAuthor();
            location = r.getLocation();
            format = r.getFormat().name();
        } else if (w instanceof Assignment a) {
            type = "ASSIGNMENT";
            description = a.getDescription();
        } else if (w instanceof Paper p) {
            type = "PAPER";
            promptOrTopic = p.getPromptOrTopic();
            wordCountTarget = p.getWordCountTarget();
            scoreOrGrade = p.getScoreOrGrade();
        } else if (w instanceof PracticeSessionItem ps) {
            type = "PRACTICE_SESSION";
            methodId = ps.getMethod() != null ? ps.getMethod().getId() : null;
            scripturePassage = ps.getScripturePassage();
            durationMinutes = ps.getDurationMinutes();
        }

        return new WorkItemExportDto(
            w.getId(), type, w.getTitle(), w.getStatus().name(),
            w.getEstimatedDuration(), w.getDueDate(), w.getCompletionDate(), w.getGeneralNotes(),
            unitId, owningTopicId, w.getCreatedAt(), w.getUpdatedAt(),
            source, author, location, format,
            description,
            promptOrTopic, wordCountTarget, scoreOrGrade,
            methodId, scripturePassage, durationMinutes
        );
    }
}
