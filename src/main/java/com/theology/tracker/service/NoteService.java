package com.theology.tracker.service;

import com.theology.tracker.dto.NoteFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class NoteService {

    public record ParentInfo(NoteParentType type, Long id, String label, String url) {}

    private final NoteRepository noteRepo;
    private final TopicRepository topicRepo;
    private final WorkItemRepository workItemRepo;
    private final CourseRepository courseRepo;
    private final UnitRepository unitRepo;
    private final MethodRepository methodRepo;

    public NoteService(
        NoteRepository noteRepo,
        TopicRepository topicRepo,
        WorkItemRepository workItemRepo,
        CourseRepository courseRepo,
        UnitRepository unitRepo,
        MethodRepository methodRepo
    ) {
        this.noteRepo = noteRepo;
        this.topicRepo = topicRepo;
        this.workItemRepo = workItemRepo;
        this.courseRepo = courseRepo;
        this.unitRepo = unitRepo;
        this.methodRepo = methodRepo;
    }

    @Transactional(readOnly = true)
    public Note findById(Long id) {
        return noteRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Note> findAll() {
        return noteRepo.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Note> findByParent(NoteParentType type, Long parentId) {
        return noteRepo.findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(type, parentId);
    }

    @Transactional(readOnly = true)
    public ParentInfo resolveParent(NoteParentType type, Long id) {
        return switch (type) {
            case COURSE -> {
                Course c = courseRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
                yield new ParentInfo(type, id, c.getTitle(), "/courses/" + id);
            }
            case UNIT -> {
                Unit u = unitRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + id));
                yield new ParentInfo(type, id,
                    u.getCourse().getTitle() + " › " + u.getTitle(),
                    "/courses/" + u.getCourse().getId());
            }
            case TOPIC -> {
                Topic t = topicRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
                yield new ParentInfo(type, id, t.getTitle(), "/topics/" + id);
            }
            case METHOD -> {
                Method m = methodRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Method not found: " + id));
                yield new ParentInfo(type, id, m.getName(), "/methods/" + id);
            }
            case SESSION -> new ParentInfo(type, id, "Study Session #" + id, "/sessions/" + id);
        };
    }

    @Transactional(readOnly = true)
    public List<Note> findTaggedByTopic(Long topicId) {
        return noteRepo.findByTopicTagId(topicId);
    }

    public void tagWithTopic(Long noteId, Long topicId) {
        Note note = findById(noteId);
        Topic topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        note.getTopicTags().add(topic);
        noteRepo.save(note);
    }

    public void untagFromTopic(Long noteId, Long topicId) {
        Note note = findById(noteId);
        note.getTopicTags().removeIf(t -> t.getId().equals(topicId));
        noteRepo.save(note);
    }

    public Note create(NoteFormDto form) {
        validate(form);
        Note note = new Note();
        applyForm(note, form);
        return noteRepo.save(note);
    }

    public Note update(Long id, NoteFormDto form) {
        Note note = findById(id);
        validate(form);
        applyForm(note, form);
        return noteRepo.save(note);
    }

    public LocalDateTime autoSave(Long id, String body) {
        Note note = findById(id);
        note.setBody(body != null ? body : "");
        return noteRepo.save(note).getUpdatedAt();
    }

    public void delete(Long id) {
        noteRepo.delete(findById(id));
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findReferenceableWorkItems() {
        return workItemRepo.findReferenceableWorkItems();
    }

    public Note attachWorkItemRef(Long noteId, Long workItemId) {
        Note note = findById(noteId);
        WorkItem item = workItemRepo.findById(workItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + workItemId));
        if (item instanceof Reading) {
            throw new IllegalArgumentException("Readings cannot be referenced from notes.");
        }
        note.getWorkItemRefs().add(item);
        return noteRepo.save(note);
    }

    public Note detachWorkItemRef(Long noteId, Long workItemId) {
        Note note = findById(noteId);
        note.getWorkItemRefs().removeIf(wi -> wi.getId().equals(workItemId));
        return noteRepo.save(note);
    }

    private void validate(NoteFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Note title is required.");
        }
        if (form.parentType() == null || form.parentType().isBlank()) {
            throw new IllegalArgumentException("Primary parent type is required.");
        }
        if (form.parentId() == null) {
            throw new IllegalArgumentException("Primary parent ID is required.");
        }
    }

    private void applyForm(Note note, NoteFormDto form) {
        note.setTitle(form.title().trim());
        note.setBody(form.body() != null ? form.body() : "");
        note.setPrimaryParentType(NoteParentType.valueOf(form.parentType().toUpperCase()));
        note.setPrimaryParentId(form.parentId());
        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            note.setTopicTags(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            note.setTopicTags(new HashSet<>());
        }
    }
}
