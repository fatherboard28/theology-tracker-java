package com.theology.tracker.service;

import com.theology.tracker.dto.NoteFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Note;
import com.theology.tracker.model.Topic;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepo;
    private final TopicRepository topicRepo;

    public NoteService(NoteRepository noteRepo, TopicRepository topicRepo) {
        this.noteRepo = noteRepo;
        this.topicRepo = topicRepo;
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
    public List<Note> findByTopic(Long topicId) {
        return noteRepo.findByTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public List<Note> findBacklinks(String noteTitle) {
        return noteRepo.findBacklinks(noteTitle);
    }

    @Transactional(readOnly = true)
    public List<Note> search(String q) {
        return noteRepo.searchByTitleOrBody(q);
    }

    public void tagWithTopic(Long noteId, Long topicId) {
        Note note = findById(noteId);
        Topic topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        note.getTopics().add(topic);
        noteRepo.save(note);
    }

    public void untagFromTopic(Long noteId, Long topicId) {
        Note note = findById(noteId);
        note.getTopics().removeIf(t -> t.getId().equals(topicId));
        noteRepo.save(note);
    }

    public Note create(NoteFormDto form) {
        validate(form);
        Note note = new Note();
        applyForm(note, form);
        return noteRepo.save(note);
    }

    public Note update(Long id, NoteFormDto form) {
        validate(form);
        Note note = findById(id);
        applyForm(note, form);
        return noteRepo.save(note);
    }

    public LocalDateTime autoSave(Long id, String body) {
        Note note = findById(id);
        note.setBody(body != null ? body : "");
        return noteRepo.save(note).getUpdatedAt();
    }

    public Note toggleStar(Long id) {
        Note note = findById(id);
        note.setStarred(!note.isStarred());
        return noteRepo.save(note);
    }

    public void delete(Long id) {
        noteRepo.delete(findById(id));
    }

    private void validate(NoteFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Note title is required.");
        }
    }

    private void applyForm(Note note, NoteFormDto form) {
        note.setTitle(form.title().trim());
        note.setBody(form.body() != null ? form.body() : "");
        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            note.setTopics(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            note.setTopics(new HashSet<>());
        }
    }
}
