package com.theology.tracker.service;

import com.theology.tracker.dto.WorkItemFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class WorkItemService {

    private final WorkItemRepository workItemRepo;
    private final ReadingRepository readingRepo;
    private final AssignmentRepository assignmentRepo;
    private final PaperRepository paperRepo;
    private final PracticeSessionItemRepository practiceRepo;
    private final UnitRepository unitRepo;
    private final TopicRepository topicRepo;
    private final MethodRepository methodRepo;
    private final ScriptureTagRepository scriptureTagRepo;
    private final UnitService unitService;
    private final ScriptureReferenceValidator scriptureValidator;

    public WorkItemService(
        WorkItemRepository workItemRepo,
        ReadingRepository readingRepo,
        AssignmentRepository assignmentRepo,
        PaperRepository paperRepo,
        PracticeSessionItemRepository practiceRepo,
        UnitRepository unitRepo,
        TopicRepository topicRepo,
        MethodRepository methodRepo,
        ScriptureTagRepository scriptureTagRepo,
        UnitService unitService,
        ScriptureReferenceValidator scriptureValidator
    ) {
        this.workItemRepo = workItemRepo;
        this.readingRepo = readingRepo;
        this.assignmentRepo = assignmentRepo;
        this.paperRepo = paperRepo;
        this.practiceRepo = practiceRepo;
        this.unitRepo = unitRepo;
        this.topicRepo = topicRepo;
        this.methodRepo = methodRepo;
        this.scriptureTagRepo = scriptureTagRepo;
        this.unitService = unitService;
        this.scriptureValidator = scriptureValidator;
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findAll() {
        return workItemRepo.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public WorkItem findById(Long id) {
        return workItemRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findByUnit(Long unitId) {
        return workItemRepo.findByUnitId(unitId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findByOwningTopic(Long topicId) {
        return workItemRepo.findByOwningTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public List<ScriptureTag> findScriptureTagsForWorkItem(Long workItemId) {
        return scriptureTagRepo.findByEntityTypeAndEntityId(ScriptureEntityType.WORK_ITEM, workItemId);
    }

    public WorkItem create(WorkItemFormDto form) {
        validateParentage(form.unitId(), form.owningTopicId());
        WorkItemType type = WorkItemType.valueOf(form.type().toUpperCase());

        WorkItem item = switch (type) {
            case READING -> buildReading(new Reading(), form);
            case ASSIGNMENT -> buildAssignment(new Assignment(), form);
            case PAPER -> buildPaper(new Paper(), form);
            case PRACTICE_SESSION -> buildPracticeSession(new PracticeSessionItem(), form);
        };

        applyCommon(item, form);
        WorkItem saved = workItemRepo.save(item);
        saveScriptureTags(saved.getId(), form.scriptureTags());
        if (saved.getUnit() != null) {
            unitService.checkAndAutoComplete(saved.getUnit().getId());
        }
        return saved;
    }

    public WorkItem update(Long id, WorkItemFormDto form) {
        WorkItem item = findById(id);
        validateParentage(form.unitId(), form.owningTopicId());
        applyCommon(item, form);
        applyTypeSpecific(item, form);
        WorkItem saved = workItemRepo.save(item);
        scriptureTagRepo.deleteByEntityTypeAndEntityId(ScriptureEntityType.WORK_ITEM, id);
        saveScriptureTags(id, form.scriptureTags());
        if (saved.getUnit() != null) {
            unitService.checkAndAutoComplete(saved.getUnit().getId());
        }
        return saved;
    }

    public void delete(Long id) {
        WorkItem item = findById(id);
        Long unitId = item.getUnit() != null ? item.getUnit().getId() : null;
        scriptureTagRepo.deleteByEntityTypeAndEntityId(ScriptureEntityType.WORK_ITEM, id);
        workItemRepo.delete(item);
        if (unitId != null) {
            unitService.checkAndAutoComplete(unitId);
        }
    }

    public WorkItem toggleComplete(Long id) {
        WorkItem item = findById(id);
        if (item.getStatus() == WorkItemStatus.COMPLETE) {
            item.setStatus(WorkItemStatus.NOT_STARTED);
            item.setCompletionDate(null);
        } else {
            item.setStatus(WorkItemStatus.COMPLETE);
            item.setCompletionDate(LocalDate.now());
        }
        WorkItem saved = workItemRepo.save(item);
        if (saved.getUnit() != null) {
            unitService.checkAndAutoComplete(saved.getUnit().getId());
        }
        return saved;
    }

    private void validateParentage(Long unitId, Long owningTopicId) {
        if (unitId == null && owningTopicId == null) {
            throw new IllegalArgumentException(
                "A work item must belong to a unit or be owned by a topic.");
        }
    }

    private void applyCommon(WorkItem item, WorkItemFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        item.setTitle(form.title().trim());

        if (form.status() != null && !form.status().isBlank()) {
            WorkItemStatus newStatus = WorkItemStatus.valueOf(form.status().toUpperCase());
            item.setStatus(newStatus);
            if (newStatus == WorkItemStatus.COMPLETE && item.getCompletionDate() == null) {
                item.setCompletionDate(LocalDate.now());
            } else if (newStatus != WorkItemStatus.COMPLETE) {
                item.setCompletionDate(null);
            }
        }

        item.setEstimatedDuration(form.estimatedDuration());
        item.setDueDate(form.dueDate());
        item.setGeneralNotes(
            form.generalNotes() != null && !form.generalNotes().isBlank()
                ? form.generalNotes().trim() : null);

        if (form.unitId() != null) {
            item.setUnit(unitRepo.findById(form.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + form.unitId())));
        } else {
            item.setUnit(null);
        }

        if (form.owningTopicId() != null) {
            item.setOwningTopic(topicRepo.findById(form.owningTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + form.owningTopicId())));
        } else {
            item.setOwningTopic(null);
        }

        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            item.setTopicTags(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            item.setTopicTags(new HashSet<>());
        }
    }

    private void applyTypeSpecific(WorkItem item, WorkItemFormDto form) {
        if (item instanceof Reading r) buildReading(r, form);
        else if (item instanceof Assignment a) buildAssignment(a, form);
        else if (item instanceof Paper p) buildPaper(p, form);
        else if (item instanceof PracticeSessionItem ps) buildPracticeSession(ps, form);
    }

    private Reading buildReading(Reading r, WorkItemFormDto form) {
        if (form.source() == null || form.source().isBlank()) {
            throw new IllegalArgumentException("Source is required for readings.");
        }
        r.setSource(form.source().trim());
        r.setAuthor(form.author() != null && !form.author().isBlank() ? form.author().trim() : null);
        r.setLocation(form.location() != null && !form.location().isBlank() ? form.location().trim() : null);
        if (form.format() != null && !form.format().isBlank()) {
            r.setFormat(ReadingFormat.valueOf(form.format().toUpperCase()));
        }
        return r;
    }

    private Assignment buildAssignment(Assignment a, WorkItemFormDto form) {
        a.setDescription(
            form.description() != null && !form.description().isBlank()
                ? form.description().trim() : null);
        return a;
    }

    private Paper buildPaper(Paper p, WorkItemFormDto form) {
        p.setPromptOrTopic(
            form.promptOrTopic() != null && !form.promptOrTopic().isBlank()
                ? form.promptOrTopic().trim() : null);
        p.setWordCountTarget(form.wordCountTarget());
        p.setScoreOrGrade(
            form.scoreOrGrade() != null && !form.scoreOrGrade().isBlank()
                ? form.scoreOrGrade().trim() : null);
        return p;
    }

    private PracticeSessionItem buildPracticeSession(PracticeSessionItem ps, WorkItemFormDto form) {
        if (form.methodId() != null) {
            ps.setMethod(methodRepo.findById(form.methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Method not found: " + form.methodId())));
        } else {
            ps.setMethod(null);
        }
        ps.setScripturePassage(
            form.scripturePassage() != null && !form.scripturePassage().isBlank()
                ? form.scripturePassage().trim() : null);
        ps.setDurationMinutes(form.durationMinutes());
        return ps;
    }

    private void saveScriptureTags(Long workItemId, List<String> refs) {
        if (refs == null || refs.isEmpty()) return;
        for (String ref : refs) {
            String trimmed = ref.trim();
            if (trimmed.isBlank()) continue;
            scriptureValidator.validate(trimmed);
            ScriptureTag tag = new ScriptureTag();
            tag.setReference(trimmed);
            tag.setEntityType(ScriptureEntityType.WORK_ITEM);
            tag.setEntityId(workItemId);
            scriptureTagRepo.save(tag);
        }
    }
}
