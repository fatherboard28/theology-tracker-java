package com.theology.tracker.service;

import com.theology.tracker.dto.DashboardActiveCourseDto;
import com.theology.tracker.dto.DashboardActiveTopicDto;
import com.theology.tracker.dto.DashboardUpcomingDueDto;
import com.theology.tracker.dto.HeatmapDayDto;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CourseRepository courseRepo;
    private final TopicRepository topicRepo;
    private final WorkItemRepository workItemRepo;
    private final NoteRepository noteRepo;
    private final StudySessionRepository sessionRepo;
    private final UnitRepository unitRepo;

    public DashboardService(CourseRepository courseRepo, TopicRepository topicRepo,
                            WorkItemRepository workItemRepo, NoteRepository noteRepo,
                            StudySessionRepository sessionRepo, UnitRepository unitRepo) {
        this.courseRepo = courseRepo;
        this.topicRepo = topicRepo;
        this.workItemRepo = workItemRepo;
        this.noteRepo = noteRepo;
        this.sessionRepo = sessionRepo;
        this.unitRepo = unitRepo;
    }

    public List<DashboardActiveCourseDto> getActiveCourses() {
        return courseRepo.findByStatus(CourseStatus.ACTIVE).stream()
            .limit(5)
            .map(c -> {
                long total = c.getUnits().stream()
                    .mapToLong(u -> workItemRepo.countByUnitId(u.getId())).sum();
                long done = c.getUnits().stream()
                    .mapToLong(u -> workItemRepo.countByUnitIdAndStatus(u.getId(), WorkItemStatus.COMPLETE)).sum();
                int pct = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
                return new DashboardActiveCourseDto(c.getId(), c.getTitle(), pct, c.getTargetCompletion());
            })
            .collect(Collectors.toList());
    }

    public List<DashboardActiveTopicDto> getActiveTopics() {
        return topicRepo.findAllByOrderByCreatedAtDesc().stream()
            .limit(5)
            .map(t -> {
                long count = workItemRepo.countOwnedByTopicId(t.getId())
                    + workItemRepo.countTaggedByTopicId(t.getId());
                return new DashboardActiveTopicDto(t.getId(), t.getTitle(), count);
            })
            .collect(Collectors.toList());
    }

    public int getWeekMinutes() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Integer sum = sessionRepo.sumDurationBetween(monday, today);
        return sum != null ? sum : 0;
    }

    public int getMonthMinutes() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        Integer sum = sessionRepo.sumDurationBetween(firstOfMonth, today);
        return sum != null ? sum : 0;
    }

    public List<DashboardUpcomingDueDto> getUpcomingDue() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);

        List<DashboardUpcomingDueDto> items = new ArrayList<>();

        workItemRepo.findByDueDateBetween(today, end).stream()
            .filter(w -> w.getStatus() != WorkItemStatus.COMPLETE)
            .forEach(w -> items.add(new DashboardUpcomingDueDto(
                w.getTitle(), w.getDueDate(), "Work Item", "/work-items/" + w.getId())));

        unitRepo.findByTargetCompletionBetweenAndActualCompletionIsNull(today, end)
            .forEach(u -> items.add(new DashboardUpcomingDueDto(
                u.getTitle(), u.getTargetCompletion(), "Unit",
                "/courses/" + u.getCourse().getId())));

        courseRepo.findByTargetCompletionBetweenAndStatusNot(today, end, CourseStatus.COMPLETE)
            .forEach(c -> items.add(new DashboardUpcomingDueDto(
                c.getTitle(), c.getTargetCompletion(), "Course", "/courses/" + c.getId())));

        items.sort(Comparator.comparing(DashboardUpcomingDueDto::dueDate));
        return items;
    }

    public List<WorkItem> getRecentWorkItems() {
        return workItemRepo.findTop5ByOrderByUpdatedAtDesc();
    }

    public List<Note> getRecentNotes() {
        return noteRepo.findTop5ByOrderByUpdatedAtDesc();
    }

    public List<HeatmapDayDto> getHeatmapDays() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(91)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<StudySession> sessions = sessionRepo.findBySessionDateBetweenOrderBySessionDateDesc(startDate, today);
        Map<LocalDate, Integer> minutesByDate = new HashMap<>();
        for (StudySession s : sessions) {
            if (s.getDurationMinutes() != null) {
                minutesByDate.merge(s.getSessionDate(), s.getDurationMinutes(), Integer::sum);
            }
        }

        LocalDate dueEnd = today.plusDays(7);
        Set<LocalDate> dueDates = new HashSet<>();
        workItemRepo.findByDueDateBetween(today, dueEnd).stream()
            .filter(w -> w.getStatus() != WorkItemStatus.COMPLETE)
            .map(WorkItem::getDueDate).forEach(dueDates::add);
        unitRepo.findByTargetCompletionBetweenAndActualCompletionIsNull(today, dueEnd)
            .stream().map(Unit::getTargetCompletion).forEach(dueDates::add);
        courseRepo.findByTargetCompletionBetweenAndStatusNot(today, dueEnd, CourseStatus.COMPLETE)
            .stream().map(Course::getTargetCompletion).forEach(dueDates::add);

        List<HeatmapDayDto> days = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(today)) {
            int minutes = minutesByDate.getOrDefault(d, 0);
            days.add(new HeatmapDayDto(d, minutesToLevel(minutes), dueDates.contains(d)));
            d = d.plusDays(1);
        }
        return days;
    }

    private int minutesToLevel(int minutes) {
        if (minutes == 0) return 0;
        if (minutes <= 30) return 1;
        if (minutes <= 60) return 2;
        if (minutes <= 120) return 3;
        return 4;
    }
}
