package com.theology.tracker.service;

import com.theology.tracker.model.StudySession;
import com.theology.tracker.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final StudySessionRepository sessionRepo;

    public ProgressService(StudySessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    public int totalMinutesForWorkItem(Long workItemId) {
        return sessionRepo.sumDurationByWorkItemId(workItemId);
    }

    public int totalMinutesForUnit(Long unitId) {
        return sessionRepo.sumDurationByUnitId(unitId);
    }

    public int totalMinutesForCourse(Long courseId) {
        return sessionRepo.sumDurationByCourseId(courseId);
    }

    public int totalMinutesForTopic(Long topicId) {
        return sessionRepo.sumDurationByTopicId(topicId);
    }

    public int calculateCurrentStreak() {
        List<LocalDate> dates = sessionRepo.findAllDistinctDatesOrderedDesc();
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate mostRecent = dates.get(0);
        if (!mostRecent.isEqual(today) && !mostRecent.isEqual(today.minusDays(1))) return 0;

        int streak = 0;
        LocalDate expected = mostRecent;
        for (LocalDate date : dates) {
            if (date.isEqual(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    public int calculateLongestStreak() {
        List<LocalDate> dates = sessionRepo.findAllDistinctDatesOrderedDesc();
        if (dates.isEmpty()) return 0;

        int longest = 1;
        int current = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).isEqual(dates.get(i - 1).minusDays(1))) {
                current++;
                if (current > longest) longest = current;
            } else {
                current = 1;
            }
        }
        return longest;
    }

    public Optional<StudySession> getLastSession() {
        return sessionRepo.findLastSession();
    }
}
