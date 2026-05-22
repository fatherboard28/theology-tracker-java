package com.theology.tracker.service;

import com.theology.tracker.dto.SearchResultDto;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.Note;
import com.theology.tracker.model.ScriptureEntityType;
import com.theology.tracker.model.ScriptureTag;
import com.theology.tracker.model.StudySession;
import com.theology.tracker.model.Topic;
import com.theology.tracker.model.Unit;
import com.theology.tracker.model.WorkItem;
import com.theology.tracker.repository.CourseRepository;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.ScriptureTagRepository;
import com.theology.tracker.repository.StudySessionRepository;
import com.theology.tracker.repository.TopicRepository;
import com.theology.tracker.repository.UnitRepository;
import com.theology.tracker.repository.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final CourseRepository courseRepo;
    private final UnitRepository unitRepo;
    private final TopicRepository topicRepo;
    private final WorkItemRepository workItemRepo;
    private final NoteRepository noteRepo;
    private final StudySessionRepository sessionRepo;
    private final ScriptureTagRepository scriptureTagRepo;
    private final ScriptureReferenceValidator scriptureValidator;

    public SearchService(
        CourseRepository courseRepo,
        UnitRepository unitRepo,
        TopicRepository topicRepo,
        WorkItemRepository workItemRepo,
        NoteRepository noteRepo,
        StudySessionRepository sessionRepo,
        ScriptureTagRepository scriptureTagRepo,
        ScriptureReferenceValidator scriptureValidator
    ) {
        this.courseRepo = courseRepo;
        this.unitRepo = unitRepo;
        this.topicRepo = topicRepo;
        this.workItemRepo = workItemRepo;
        this.noteRepo = noteRepo;
        this.sessionRepo = sessionRepo;
        this.scriptureTagRepo = scriptureTagRepo;
        this.scriptureValidator = scriptureValidator;
    }

    public SearchResultDto search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResultDto(query, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        String q = query.trim();

        List<Course> courses = courseRepo.searchByTitleOrDescription(q);
        List<Unit> units = unitRepo.searchByTitleOrDescription(q);
        List<Topic> topics = topicRepo.searchByTitleOrDescription(q);
        List<WorkItem> workItems = new ArrayList<>(workItemRepo.searchByTitleOrNotes(q));
        List<Note> notes = noteRepo.searchByTitleOrBody(q);
        List<StudySession> sessions = new ArrayList<>(sessionRepo.searchByReflectionNote(q));

        if (looksLikeScriptureQuery(q)) {
            List<ScriptureTag> matchingTags = resolveScriptureTags(q);

            Set<Long> scriptureWorkItemIds = matchingTags.stream()
                .filter(t -> t.getEntityType() == ScriptureEntityType.WORK_ITEM)
                .map(ScriptureTag::getEntityId)
                .collect(Collectors.toSet());

            Set<Long> scriptureSessionIds = matchingTags.stream()
                .filter(t -> t.getEntityType() == ScriptureEntityType.SESSION)
                .map(ScriptureTag::getEntityId)
                .collect(Collectors.toSet());

            if (!scriptureWorkItemIds.isEmpty()) {
                Set<Long> existingWorkItemIds = workItems.stream()
                    .map(WorkItem::getId)
                    .collect(Collectors.toSet());
                workItemRepo.findAllById(scriptureWorkItemIds).stream()
                    .filter(wi -> !existingWorkItemIds.contains(wi.getId()))
                    .forEach(workItems::add);
                workItems.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
            }

            if (!scriptureSessionIds.isEmpty()) {
                Set<Long> existingSessionIds = sessions.stream()
                    .map(StudySession::getId)
                    .collect(Collectors.toSet());
                sessionRepo.findAllById(scriptureSessionIds).stream()
                    .filter(s -> !existingSessionIds.contains(s.getId()))
                    .forEach(sessions::add);
                sessions.sort((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()));
            }
        }

        return new SearchResultDto(q, courses, units, topics, workItems, notes, sessions);
    }

    private boolean looksLikeScriptureQuery(String q) {
        for (String abbr : ScriptureReferenceValidator.BOOK_ABBREVIATIONS) {
            if (q.startsWith(abbr + " ") && q.length() > abbr.length() + 1) {
                String rest = q.substring(abbr.length() + 1);
                return !rest.isEmpty() && Character.isDigit(rest.charAt(0));
            }
        }
        return false;
    }

    private List<ScriptureTag> resolveScriptureTags(String q) {
        if (scriptureValidator.isChapterQuery(q)) {
            List<ScriptureTag> exact = scriptureTagRepo.findByReference(q);
            List<ScriptureTag> verseTags = scriptureTagRepo.findByReferenceStartingWith(q + ":");
            return Stream.concat(exact.stream(), verseTags.stream()).distinct().collect(Collectors.toList());
        } else {
            List<ScriptureTag> exact = scriptureTagRepo.findByReference(q);
            List<ScriptureTag> rangeTags = scriptureTagRepo.findByReferenceStartingWith(q);
            return Stream.concat(exact.stream(), rangeTags.stream()).distinct().collect(Collectors.toList());
        }
    }
}
