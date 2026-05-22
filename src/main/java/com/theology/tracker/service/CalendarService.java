package com.theology.tracker.service;

import com.theology.tracker.dto.CalendarDayDto;
import com.theology.tracker.dto.CalendarEventDto;
import com.theology.tracker.dto.CalendarSessionDto;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final StudySessionRepository sessionRepo;
    private final WorkItemRepository workItemRepo;
    private final UnitRepository unitRepo;
    private final CourseRepository courseRepo;

    public CalendarService(StudySessionRepository sessionRepo, WorkItemRepository workItemRepo,
                           UnitRepository unitRepo, CourseRepository courseRepo) {
        this.sessionRepo = sessionRepo;
        this.workItemRepo = workItemRepo;
        this.unitRepo = unitRepo;
        this.courseRepo = courseRepo;
    }

    public List<CalendarDayDto> getMonthDays(YearMonth month) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        LocalDate gridStart = firstDay.with(DayOfWeek.MONDAY);
        if (gridStart.isAfter(firstDay)) gridStart = gridStart.minusWeeks(1);

        LocalDate gridEnd = gridStart.plusDays(34);
        while (gridEnd.isBefore(lastDay)) gridEnd = gridEnd.plusDays(7);

        return buildDayDtos(gridStart, gridEnd, today, firstDay.getMonth());
    }

    public List<CalendarDayDto> getWeekDays(LocalDate weekDate) {
        LocalDate today = LocalDate.now();
        LocalDate monday = weekDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        return buildDayDtos(monday, sunday, today, null);
    }

    public List<CalendarEventDto> getAgendaItems() {
        LocalDate today = LocalDate.now();
        LocalDate lookback = today.minusYears(1);
        LocalDate end = today.plusMonths(3);

        List<CalendarEventDto> items = new ArrayList<>();

        workItemRepo.findByDueDateBeforeAndStatusNot(today, WorkItemStatus.COMPLETE)
            .forEach(w -> items.add(toDueEvent(w, true)));

        unitRepo.findByTargetCompletionBetween(lookback, today.minusDays(1)).stream()
            .filter(u -> u.getActualCompletion() == null)
            .forEach(u -> items.add(toUnitEvent(u, true)));

        courseRepo.findByTargetCompletionBetween(lookback, today.minusDays(1)).stream()
            .filter(c -> c.getStatus() != CourseStatus.COMPLETE)
            .forEach(c -> items.add(toCourseEvent(c, true)));

        workItemRepo.findByDueDateBetween(today, end).stream()
            .filter(w -> w.getStatus() != WorkItemStatus.COMPLETE)
            .forEach(w -> items.add(toDueEvent(w, false)));

        unitRepo.findByTargetCompletionBetween(today, end).stream()
            .filter(u -> u.getActualCompletion() == null)
            .forEach(u -> items.add(toUnitEvent(u, false)));

        courseRepo.findByTargetCompletionBetween(today, end).stream()
            .filter(c -> c.getStatus() != CourseStatus.COMPLETE)
            .forEach(c -> items.add(toCourseEvent(c, false)));

        items.sort(Comparator.comparing(CalendarEventDto::date).thenComparing(CalendarEventDto::entityType));
        return items;
    }

    private List<CalendarDayDto> buildDayDtos(LocalDate start, LocalDate end, LocalDate today, Month currentMonth) {
        List<StudySession> sessions = sessionRepo.findBySessionDateBetweenOrderBySessionDateDesc(start, end);
        List<WorkItem> completedItems = workItemRepo.findByCompletionDateBetween(start, end);
        List<WorkItem> dueItems = workItemRepo.findByDueDateBetween(start, end);
        List<Unit> dueUnits = unitRepo.findByTargetCompletionBetween(start, end);
        List<Course> dueCourses = courseRepo.findByTargetCompletionBetween(start, end);

        Map<LocalDate, Integer> minutesByDate = new HashMap<>();
        Map<LocalDate, List<StudySession>> sessionsByDate = new HashMap<>();
        for (StudySession s : sessions) {
            if (s.getDurationMinutes() != null) {
                minutesByDate.merge(s.getSessionDate(), s.getDurationMinutes(), Integer::sum);
                sessionsByDate.computeIfAbsent(s.getSessionDate(), k -> new ArrayList<>()).add(s);
            }
        }

        Map<LocalDate, List<WorkItem>> completedByDate = completedItems.stream()
            .filter(w -> w.getCompletionDate() != null)
            .collect(Collectors.groupingBy(WorkItem::getCompletionDate));

        Map<LocalDate, List<WorkItem>> dueByDate = dueItems.stream()
            .filter(w -> w.getDueDate() != null)
            .collect(Collectors.groupingBy(WorkItem::getDueDate));

        Map<LocalDate, List<Unit>> unitsByDate = dueUnits.stream()
            .filter(u -> u.getTargetCompletion() != null)
            .collect(Collectors.groupingBy(Unit::getTargetCompletion));

        Map<LocalDate, List<Course>> coursesByDate = dueCourses.stream()
            .filter(c -> c.getTargetCompletion() != null)
            .collect(Collectors.groupingBy(Course::getTargetCompletion));

        List<CalendarDayDto> days = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            int minutes = minutesByDate.getOrDefault(d, 0);

            List<CalendarSessionDto> daySessions = sessionsByDate.getOrDefault(d, List.of()).stream()
                .map(s -> new CalendarSessionDto(
                    s.getId(),
                    s.getDurationMinutes() != null ? s.getDurationMinutes() : 0,
                    s.getWorkItem() != null ? s.getWorkItem().getTitle() : null))
                .collect(Collectors.toList());

            List<CalendarEventDto> completed = completedByDate.getOrDefault(d, List.of()).stream()
                .map(this::toCompletedEvent)
                .collect(Collectors.toList());

            List<CalendarEventDto> due = new ArrayList<>();
            final LocalDate finalD = d;
            dueByDate.getOrDefault(d, List.of()).stream()
                .filter(w -> w.getStatus() != WorkItemStatus.COMPLETE)
                .map(w -> toDueEvent(w, finalD.isBefore(today)))
                .forEach(due::add);
            unitsByDate.getOrDefault(d, List.of()).stream()
                .map(u -> toUnitEvent(u, finalD.isBefore(today) && u.getActualCompletion() == null))
                .forEach(due::add);
            coursesByDate.getOrDefault(d, List.of()).stream()
                .map(c -> toCourseEvent(c, finalD.isBefore(today) && c.getStatus() != CourseStatus.COMPLETE))
                .forEach(due::add);

            boolean inCurrentMonth = currentMonth == null || d.getMonth() == currentMonth;
            days.add(new CalendarDayDto(d, minutesToLevel(minutes), minutes, daySessions.size(),
                daySessions, completed, due, d.equals(today), inCurrentMonth));
            d = d.plusDays(1);
        }
        return days;
    }

    private CalendarEventDto toCompletedEvent(WorkItem w) {
        return new CalendarEventDto(w.getId(), null, w.getTitle(), "WorkItem",
            "/work-items/" + w.getId(), w.getCompletionDate(), false);
    }

    private CalendarEventDto toDueEvent(WorkItem w, boolean overdue) {
        return new CalendarEventDto(w.getId(), null, w.getTitle(), "WorkItem",
            "/work-items/" + w.getId(), w.getDueDate(), overdue);
    }

    private CalendarEventDto toUnitEvent(Unit u, boolean overdue) {
        return new CalendarEventDto(u.getId(), u.getCourse().getId(), u.getTitle(), "Unit",
            "/courses/" + u.getCourse().getId(), u.getTargetCompletion(), overdue);
    }

    private CalendarEventDto toCourseEvent(Course c, boolean overdue) {
        return new CalendarEventDto(c.getId(), null, c.getTitle(), "Course",
            "/courses/" + c.getId(), c.getTargetCompletion(), overdue);
    }

    private int minutesToLevel(int minutes) {
        if (minutes == 0) return 0;
        if (minutes <= 30) return 1;
        if (minutes <= 60) return 2;
        if (minutes <= 120) return 3;
        return 4;
    }
}
