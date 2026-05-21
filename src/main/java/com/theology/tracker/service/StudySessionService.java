package com.theology.tracker.service;

import com.theology.tracker.dto.StudySessionFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudySessionService {

    private final StudySessionRepository sessionRepo;
    private final WorkItemRepository workItemRepo;
    private final MethodRepository methodRepo;
    private final TopicRepository topicRepo;
    private final ScriptureTagRepository scriptureTagRepo;
    private final ScriptureReferenceValidator scriptureValidator;

    public StudySessionService(
        StudySessionRepository sessionRepo,
        WorkItemRepository workItemRepo,
        MethodRepository methodRepo,
        TopicRepository topicRepo,
        ScriptureTagRepository scriptureTagRepo,
        ScriptureReferenceValidator scriptureValidator
    ) {
        this.sessionRepo = sessionRepo;
        this.workItemRepo = workItemRepo;
        this.methodRepo = methodRepo;
        this.topicRepo = topicRepo;
        this.scriptureTagRepo = scriptureTagRepo;
        this.scriptureValidator = scriptureValidator;
    }

    public List<StudySession> findAll() {
        return sessionRepo.findAllByOrderBySessionDateDesc();
    }

    public StudySession findById(Long id) {
        return sessionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
    }

    public List<StudySession> findByWorkItem(Long workItemId) {
        return sessionRepo.findByWorkItemIdOrderBySessionDateDesc(workItemId);
    }

    public List<StudySession> findByMethod(Long methodId) {
        return sessionRepo.findByMethodIdOrderBySessionDateDesc(methodId);
    }

    public List<StudySession> findByTopic(Long topicId) {
        return sessionRepo.findByTopics_IdOrderBySessionDateDesc(topicId);
    }

    public List<ScriptureTag> findScriptureTags(Long sessionId) {
        return scriptureTagRepo.findByEntityTypeAndEntityId(ScriptureEntityType.SESSION, sessionId);
    }

    @Transactional
    public StudySession create(StudySessionFormDto form) {
        validate(form);
        StudySession session = new StudySession();
        applyForm(session, form);
        StudySession saved = sessionRepo.save(session);
        saveScriptureTags(saved.getId(), form.scriptureTags());
        return saved;
    }

    @Transactional
    public StudySession update(Long id, StudySessionFormDto form) {
        validate(form);
        StudySession session = findById(id);
        applyForm(session, form);
        StudySession saved = sessionRepo.save(session);
        scriptureTagRepo.deleteByEntityTypeAndEntityId(ScriptureEntityType.SESSION, id);
        saveScriptureTags(id, form.scriptureTags());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        StudySession session = findById(id);
        scriptureTagRepo.deleteByEntityTypeAndEntityId(ScriptureEntityType.SESSION, id);
        sessionRepo.delete(session);
    }

    private void validate(StudySessionFormDto form) {
        if (form.sessionDate() == null) {
            throw new IllegalArgumentException("Session date is required.");
        }
        if (form.durationMinutes() == null || form.durationMinutes() <= 0) {
            throw new IllegalArgumentException("Duration must be a positive number of minutes.");
        }
    }

    private void applyForm(StudySession session, StudySessionFormDto form) {
        session.setSessionDate(form.sessionDate());
        session.setDurationMinutes(form.durationMinutes());

        if (form.workItemId() != null) {
            session.setWorkItem(workItemRepo.findById(form.workItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + form.workItemId())));
        } else {
            session.setWorkItem(null);
        }

        if (form.methodId() != null) {
            session.setMethod(methodRepo.findById(form.methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Method not found: " + form.methodId())));
        } else {
            session.setMethod(null);
        }

        session.setReflectionNote(
            form.reflectionNote() != null && !form.reflectionNote().isBlank()
                ? form.reflectionNote().trim() : null);

        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            session.setTopics(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            session.setTopics(new HashSet<>());
        }
    }

    private void saveScriptureTags(Long sessionId, List<String> refs) {
        if (refs == null || refs.isEmpty()) return;
        for (String ref : refs) {
            String trimmed = ref.trim();
            if (trimmed.isBlank()) continue;
            scriptureValidator.validate(trimmed);
            ScriptureTag tag = new ScriptureTag();
            tag.setReference(trimmed);
            tag.setEntityType(ScriptureEntityType.SESSION);
            tag.setEntityId(sessionId);
            scriptureTagRepo.save(tag);
        }
    }
}
