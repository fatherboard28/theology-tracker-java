package com.theology.tracker.service;

import com.theology.tracker.dto.export.*;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.CourseRepository;
import com.theology.tracker.repository.MethodRepository;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.ScriptureTagRepository;
import com.theology.tracker.repository.StudySessionRepository;
import com.theology.tracker.repository.TopicRepository;
import com.theology.tracker.repository.UnitRepository;
import com.theology.tracker.repository.WorkItemRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataImportService {

    private final JdbcTemplate jdbc;
    private final CourseRepository courseRepo;
    private final UnitRepository unitRepo;
    private final TopicRepository topicRepo;
    private final WorkItemRepository workItemRepo;
    private final NoteRepository noteRepo;
    private final StudySessionRepository sessionRepo;
    private final MethodRepository methodRepo;
    private final ScriptureTagRepository scriptureTagRepo;

    public DataImportService(
        JdbcTemplate jdbc,
        CourseRepository courseRepo,
        UnitRepository unitRepo,
        TopicRepository topicRepo,
        WorkItemRepository workItemRepo,
        NoteRepository noteRepo,
        StudySessionRepository sessionRepo,
        MethodRepository methodRepo,
        ScriptureTagRepository scriptureTagRepo
    ) {
        this.jdbc = jdbc;
        this.courseRepo = courseRepo;
        this.unitRepo = unitRepo;
        this.topicRepo = topicRepo;
        this.workItemRepo = workItemRepo;
        this.noteRepo = noteRepo;
        this.sessionRepo = sessionRepo;
        this.methodRepo = methodRepo;
        this.scriptureTagRepo = scriptureTagRepo;
    }

    @Transactional
    public void importData(FullExportDto data) {
        clearAll();

        Map<Long, Method> methodMap = importMethods(data.methods());
        Map<Long, Topic> topicMap = importTopics(data.topics());
        Map<Long, Course> courseMap = importCourses(data.courses());
        Map<Long, Unit> unitMap = importUnits(data.units(), courseMap);
        Map<Long, WorkItem> workItemMap = importWorkItems(data.workItems(), unitMap, topicMap, methodMap);
        Map<Long, StudySession> sessionMap = importSessions(data.sessions(), workItemMap, methodMap);
        Map<Long, Note> noteMap = importNotes(data.notes(), courseMap, unitMap, topicMap, methodMap, sessionMap);
        importScriptureTags(data.scriptureTags(), workItemMap, topicMap, sessionMap);
        applyJoins(data, courseMap, unitMap, workItemMap, noteMap, sessionMap, topicMap);
    }

    private void clearAll() {
        jdbc.execute("DELETE FROM course_topics");
        jdbc.execute("DELETE FROM unit_topics");
        jdbc.execute("DELETE FROM work_item_topics");
        jdbc.execute("DELETE FROM note_topics");
        jdbc.execute("DELETE FROM session_topics");
        jdbc.execute("DELETE FROM note_work_items");
        jdbc.execute("DELETE FROM scripture_tags");
        jdbc.execute("DELETE FROM study_sessions");
        jdbc.execute("DELETE FROM notes");
        jdbc.execute("DELETE FROM practice_session_items");
        jdbc.execute("DELETE FROM papers");
        jdbc.execute("DELETE FROM assignments");
        jdbc.execute("DELETE FROM readings");
        jdbc.execute("DELETE FROM work_items");
        jdbc.execute("DELETE FROM units");
        jdbc.execute("DELETE FROM courses");
        jdbc.execute("UPDATE topics SET parent_topic_id = NULL");
        jdbc.execute("DELETE FROM topics");
        jdbc.execute("DELETE FROM methods");
    }

    private Map<Long, Method> importMethods(List<MethodExportDto> dtos) {
        Map<Long, Method> map = new HashMap<>();
        for (MethodExportDto dto : dtos) {
            Method m = new Method();
            m.setName(dto.name());
            m.setDescription(dto.description());
            m.setPersonalNotes(dto.personalNotes());
            map.put(dto.id(), methodRepo.save(m));
        }
        return map;
    }

    private Map<Long, Topic> importTopics(List<TopicExportDto> dtos) {
        Map<Long, Topic> map = new HashMap<>();
        for (TopicExportDto dto : dtos) {
            Topic t = new Topic();
            t.setTitle(dto.title());
            t.setDescription(dto.description());
            t.setType(TopicType.valueOf(dto.type()));
            map.put(dto.id(), topicRepo.save(t));
        }
        for (TopicExportDto dto : dtos) {
            if (dto.parentTopicId() != null) {
                Topic t = map.get(dto.id());
                t.setParentTopic(map.get(dto.parentTopicId()));
                topicRepo.save(t);
            }
        }
        return map;
    }

    private Map<Long, Course> importCourses(List<CourseExportDto> dtos) {
        Map<Long, Course> map = new HashMap<>();
        for (CourseExportDto dto : dtos) {
            Course c = new Course();
            c.setTitle(dto.title());
            c.setDescription(dto.description());
            c.setStatus(CourseStatus.valueOf(dto.status()));
            c.setStartDate(dto.startDate());
            c.setTargetCompletion(dto.targetCompletion());
            c.setActualCompletion(dto.actualCompletion());
            map.put(dto.id(), courseRepo.save(c));
        }
        return map;
    }

    private Map<Long, Unit> importUnits(List<UnitExportDto> dtos, Map<Long, Course> courseMap) {
        Map<Long, Unit> map = new HashMap<>();
        for (UnitExportDto dto : dtos) {
            Unit u = new Unit();
            u.setCourse(courseMap.get(dto.courseId()));
            u.setTitle(dto.title());
            u.setDescription(dto.description());
            u.setUnitOrder(dto.unitOrder());
            u.setTargetCompletion(dto.targetCompletion());
            u.setActualCompletion(dto.actualCompletion());
            map.put(dto.id(), unitRepo.save(u));
        }
        return map;
    }

    private Map<Long, WorkItem> importWorkItems(
        List<WorkItemExportDto> dtos,
        Map<Long, Unit> unitMap,
        Map<Long, Topic> topicMap,
        Map<Long, Method> methodMap
    ) {
        Map<Long, WorkItem> map = new HashMap<>();
        for (WorkItemExportDto dto : dtos) {
            WorkItem w = switch (dto.workItemType()) {
                case "READING" -> buildReading(dto);
                case "ASSIGNMENT" -> buildAssignment(dto);
                case "PAPER" -> buildPaper(dto);
                case "PRACTICE_SESSION" -> buildPracticeSession(dto, methodMap);
                default -> throw new IllegalArgumentException("Unknown work item type: " + dto.workItemType());
            };
            w.setTitle(dto.title());
            w.setStatus(WorkItemStatus.valueOf(dto.status()));
            w.setEstimatedDuration(dto.estimatedDuration());
            w.setDueDate(dto.dueDate());
            w.setCompletionDate(dto.completionDate());
            w.setGeneralNotes(dto.generalNotes());
            if (dto.unitId() != null) w.setUnit(unitMap.get(dto.unitId()));
            if (dto.owningTopicId() != null) w.setOwningTopic(topicMap.get(dto.owningTopicId()));
            map.put(dto.id(), workItemRepo.save(w));
        }
        return map;
    }

    private Reading buildReading(WorkItemExportDto dto) {
        Reading r = new Reading();
        r.setSource(dto.source());
        r.setAuthor(dto.author());
        r.setLocation(dto.location());
        r.setFormat(dto.format() != null ? ReadingFormat.valueOf(dto.format()) : ReadingFormat.PHYSICAL_BOOK);
        return r;
    }

    private Assignment buildAssignment(WorkItemExportDto dto) {
        Assignment a = new Assignment();
        a.setDescription(dto.description());
        return a;
    }

    private Paper buildPaper(WorkItemExportDto dto) {
        Paper p = new Paper();
        p.setPromptOrTopic(dto.promptOrTopic());
        p.setWordCountTarget(dto.wordCountTarget());
        p.setScoreOrGrade(dto.scoreOrGrade());
        return p;
    }

    private PracticeSessionItem buildPracticeSession(WorkItemExportDto dto, Map<Long, Method> methodMap) {
        PracticeSessionItem ps = new PracticeSessionItem();
        if (dto.methodId() != null) ps.setMethod(methodMap.get(dto.methodId()));
        ps.setScripturePassage(dto.scripturePassage());
        ps.setDurationMinutes(dto.durationMinutes());
        return ps;
    }

    private Map<Long, StudySession> importSessions(
        List<SessionExportDto> dtos,
        Map<Long, WorkItem> workItemMap,
        Map<Long, Method> methodMap
    ) {
        Map<Long, StudySession> map = new HashMap<>();
        for (SessionExportDto dto : dtos) {
            StudySession s = new StudySession();
            s.setSessionDate(dto.sessionDate());
            s.setDurationMinutes(dto.durationMinutes());
            s.setReflectionNote(dto.reflectionNote());
            if (dto.workItemId() != null) s.setWorkItem(workItemMap.get(dto.workItemId()));
            if (dto.methodId() != null) s.setMethod(methodMap.get(dto.methodId()));
            map.put(dto.id(), sessionRepo.save(s));
        }
        return map;
    }

    private Map<Long, Note> importNotes(
        List<NoteExportDto> dtos,
        Map<Long, Course> courseMap,
        Map<Long, Unit> unitMap,
        Map<Long, Topic> topicMap,
        Map<Long, Method> methodMap,
        Map<Long, StudySession> sessionMap
    ) {
        Map<Long, Note> map = new HashMap<>();
        for (NoteExportDto dto : dtos) {
            Note n = new Note();
            n.setTitle(dto.title());
            n.setBody(dto.body() != null ? dto.body() : "");
            NoteParentType parentType = NoteParentType.valueOf(dto.primaryParentType());
            n.setPrimaryParentType(parentType);
            n.setPrimaryParentId(resolveParentId(parentType, dto.primaryParentId(),
                courseMap, unitMap, topicMap, methodMap, sessionMap));
            map.put(dto.id(), noteRepo.save(n));
        }
        return map;
    }

    private Long resolveParentId(
        NoteParentType type, Long oldId,
        Map<Long, Course> courseMap,
        Map<Long, Unit> unitMap,
        Map<Long, Topic> topicMap,
        Map<Long, Method> methodMap,
        Map<Long, StudySession> sessionMap
    ) {
        return switch (type) {
            case COURSE -> courseMap.containsKey(oldId) ? courseMap.get(oldId).getId() : oldId;
            case UNIT -> unitMap.containsKey(oldId) ? unitMap.get(oldId).getId() : oldId;
            case TOPIC -> topicMap.containsKey(oldId) ? topicMap.get(oldId).getId() : oldId;
            case METHOD -> methodMap.containsKey(oldId) ? methodMap.get(oldId).getId() : oldId;
            case SESSION -> sessionMap.containsKey(oldId) ? sessionMap.get(oldId).getId() : oldId;
        };
    }

    private void importScriptureTags(
        List<ScriptureTagExportDto> dtos,
        Map<Long, WorkItem> workItemMap,
        Map<Long, Topic> topicMap,
        Map<Long, StudySession> sessionMap
    ) {
        for (ScriptureTagExportDto dto : dtos) {
            ScriptureEntityType entityType = ScriptureEntityType.valueOf(dto.entityType());
            Long newEntityId = switch (entityType) {
                case WORK_ITEM -> workItemMap.containsKey(dto.entityId()) ? workItemMap.get(dto.entityId()).getId() : dto.entityId();
                case TOPIC -> topicMap.containsKey(dto.entityId()) ? topicMap.get(dto.entityId()).getId() : dto.entityId();
                case SESSION -> sessionMap.containsKey(dto.entityId()) ? sessionMap.get(dto.entityId()).getId() : dto.entityId();
            };
            ScriptureTag st = new ScriptureTag();
            st.setReference(dto.reference());
            st.setEntityType(entityType);
            st.setEntityId(newEntityId);
            scriptureTagRepo.save(st);
        }
    }

    private void applyJoins(
        FullExportDto data,
        Map<Long, Course> courseMap,
        Map<Long, Unit> unitMap,
        Map<Long, WorkItem> workItemMap,
        Map<Long, Note> noteMap,
        Map<Long, StudySession> sessionMap,
        Map<Long, Topic> topicMap
    ) {
        for (JoinRecordDto j : data.courseTopics()) {
            Course c = courseMap.get(j.leftId());
            Topic t = topicMap.get(j.rightId());
            if (c != null && t != null) { c.getTopics().add(t); courseRepo.save(c); }
        }
        for (JoinRecordDto j : data.unitTopics()) {
            Unit u = unitMap.get(j.leftId());
            Topic t = topicMap.get(j.rightId());
            if (u != null && t != null) { u.getTopics().add(t); unitRepo.save(u); }
        }
        for (JoinRecordDto j : data.workItemTopics()) {
            WorkItem w = workItemMap.get(j.leftId());
            Topic t = topicMap.get(j.rightId());
            if (w != null && t != null) { w.getTopicTags().add(t); workItemRepo.save(w); }
        }
        for (JoinRecordDto j : data.noteTopics()) {
            Note n = noteMap.get(j.leftId());
            Topic t = topicMap.get(j.rightId());
            if (n != null && t != null) { n.getTopicTags().add(t); noteRepo.save(n); }
        }
        for (JoinRecordDto j : data.sessionTopics()) {
            StudySession s = sessionMap.get(j.leftId());
            Topic t = topicMap.get(j.rightId());
            if (s != null && t != null) { s.getTopics().add(t); sessionRepo.save(s); }
        }
        for (JoinRecordDto j : data.noteWorkItems()) {
            Note n = noteMap.get(j.leftId());
            WorkItem w = workItemMap.get(j.rightId());
            if (n != null && w != null) { n.getWorkItemRefs().add(w); noteRepo.save(n); }
        }
    }
}
