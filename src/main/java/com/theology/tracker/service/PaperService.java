package com.theology.tracker.service;

import com.theology.tracker.dto.PaperFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Paper;
import com.theology.tracker.model.PaperStatus;
import com.theology.tracker.model.Topic;
import com.theology.tracker.repository.PaperRepository;
import com.theology.tracker.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class PaperService {

    private final PaperRepository paperRepo;
    private final TopicRepository topicRepo;

    public PaperService(PaperRepository paperRepo, TopicRepository topicRepo) {
        this.paperRepo = paperRepo;
        this.topicRepo = topicRepo;
    }

    @Transactional(readOnly = true)
    public Paper findById(Long id) {
        return paperRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Paper> findAll() {
        return paperRepo.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Paper> findByTopic(Long topicId) {
        return paperRepo.findByTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public List<Paper> search(String q) {
        return paperRepo.searchByTitleOrThesis(q);
    }

    public Paper create(PaperFormDto form, String defaultAuthor) {
        Paper paper = new Paper();
        paper.setAuthor(defaultAuthor);
        applyForm(paper, form);
        return paperRepo.save(paper);
    }

    public Paper update(Long id, PaperFormDto form) {
        Paper paper = findById(id);
        applyForm(paper, form);
        return paperRepo.save(paper);
    }

    public LocalDateTime saveBody(Long id, String body, String footnotes, String bibliography, int wordCount) {
        Paper paper = findById(id);
        paper.setBody(body != null ? body : "{}");
        paper.setFootnotes(footnotes != null ? footnotes : "[]");
        paper.setBibliography(bibliography != null ? bibliography : "[]");
        paper.setWordCount(wordCount);
        return paperRepo.save(paper).getUpdatedAt();
    }

    public void tagWithTopic(Long paperId, Long topicId) {
        Paper paper = findById(paperId);
        Topic topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        paper.getTopics().add(topic);
        paperRepo.save(paper);
    }

    public void untagFromTopic(Long paperId, Long topicId) {
        Paper paper = findById(paperId);
        paper.getTopics().removeIf(t -> t.getId().equals(topicId));
        paperRepo.save(paper);
    }

    public void delete(Long id) {
        paperRepo.delete(findById(id));
    }

    private void applyForm(Paper paper, PaperFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Paper title is required.");
        }
        paper.setTitle(form.title().trim());
        paper.setThesis(form.thesis() != null && !form.thesis().isBlank() ? form.thesis().trim() : null);
        paper.setAuthor(form.author() != null && !form.author().isBlank() ? form.author().trim() : paper.getAuthor());
        if (form.status() != null && !form.status().isBlank()) {
            paper.setStatus(PaperStatus.valueOf(form.status().toUpperCase()));
        }
        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            paper.setTopics(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            paper.setTopics(new HashSet<>());
        }
    }
}
