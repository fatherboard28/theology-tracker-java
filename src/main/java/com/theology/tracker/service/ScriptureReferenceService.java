package com.theology.tracker.service;

import com.theology.tracker.dto.ScriptureReferenceResultDto;
import com.theology.tracker.model.ScriptureEntityType;
import com.theology.tracker.model.ScriptureTag;
import com.theology.tracker.model.StudySession;
import com.theology.tracker.model.WorkItem;
import com.theology.tracker.repository.ScriptureTagRepository;
import com.theology.tracker.repository.StudySessionRepository;
import com.theology.tracker.repository.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ScriptureReferenceService {

    private final ScriptureTagRepository scriptureTagRepo;
    private final WorkItemRepository workItemRepo;
    private final StudySessionRepository sessionRepo;
    private final ScriptureReferenceValidator validator;

    public ScriptureReferenceService(
        ScriptureTagRepository scriptureTagRepo,
        WorkItemRepository workItemRepo,
        StudySessionRepository sessionRepo,
        ScriptureReferenceValidator validator
    ) {
        this.scriptureTagRepo = scriptureTagRepo;
        this.workItemRepo = workItemRepo;
        this.sessionRepo = sessionRepo;
        this.validator = validator;
    }

    public ScriptureReferenceResultDto findByReference(String query) {
        if (query == null || query.isBlank()) {
            return new ScriptureReferenceResultDto(query, List.of(), List.of());
        }

        String trimmed = query.trim();
        List<ScriptureTag> matchingTags = resolveMatchingTags(trimmed);

        Set<Long> workItemIds = matchingTags.stream()
            .filter(t -> t.getEntityType() == ScriptureEntityType.WORK_ITEM)
            .map(ScriptureTag::getEntityId)
            .collect(Collectors.toSet());

        Set<Long> sessionIds = matchingTags.stream()
            .filter(t -> t.getEntityType() == ScriptureEntityType.SESSION)
            .map(ScriptureTag::getEntityId)
            .collect(Collectors.toSet());

        List<WorkItem> workItems = workItemIds.isEmpty()
            ? List.of()
            : workItemRepo.findAllById(workItemIds).stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .toList();

        List<StudySession> sessions = sessionIds.isEmpty()
            ? List.of()
            : sessionRepo.findAllById(sessionIds).stream()
                .sorted((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()))
                .toList();

        return new ScriptureReferenceResultDto(trimmed, workItems, sessions);
    }

    private List<ScriptureTag> resolveMatchingTags(String query) {
        if (validator.isChapterQuery(query)) {
            // Chapter-level: match "Rom 8" exactly and all "Rom 8:*" verse tags
            List<ScriptureTag> exact = scriptureTagRepo.findByReference(query);
            List<ScriptureTag> verseTags = scriptureTagRepo.findByReferenceStartingWith(query + ":");
            return Stream.concat(exact.stream(), verseTags.stream())
                .distinct()
                .collect(Collectors.toList());
        } else {
            // Verse-level: match exactly and any range starting with this verse
            List<ScriptureTag> exact = scriptureTagRepo.findByReference(query);
            List<ScriptureTag> rangeTags = scriptureTagRepo.findByReferenceStartingWith(query);
            return Stream.concat(exact.stream(), rangeTags.stream())
                .distinct()
                .collect(Collectors.toList());
        }
    }
}
