package com.theology.tracker.service;

import com.theology.tracker.dto.SearchResultDto;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.PaperRepository;
import com.theology.tracker.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final TopicRepository topicRepo;
    private final NoteRepository noteRepo;
    private final PaperRepository paperRepo;

    public SearchService(TopicRepository topicRepo, NoteRepository noteRepo, PaperRepository paperRepo) {
        this.topicRepo = topicRepo;
        this.noteRepo = noteRepo;
        this.paperRepo = paperRepo;
    }

    public SearchResultDto search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResultDto(query, java.util.List.of(), java.util.List.of(), java.util.List.of());
        }
        String q = query.trim();
        return new SearchResultDto(
            q,
            topicRepo.searchByTitleOrDescription(q),
            noteRepo.searchByTitleOrBody(q),
            paperRepo.searchByTitleOrThesis(q)
        );
    }
}
