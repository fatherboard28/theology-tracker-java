package com.theology.tracker.repository;

import com.theology.tracker.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findAllByOrderBySessionDateDesc();

    List<StudySession> findBySessionDateBetweenOrderBySessionDateDesc(LocalDate from, LocalDate to);

    List<StudySession> findByWorkItemIdOrderBySessionDateDesc(Long workItemId);

    List<StudySession> findByMethodIdOrderBySessionDateDesc(Long methodId);

    List<StudySession> findByTopics_IdOrderBySessionDateDesc(Long topicId);

    @Query("SELECT SUM(s.durationMinutes) FROM StudySession s WHERE s.sessionDate BETWEEN :from AND :to")
    Integer sumDurationBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT DISTINCT s.sessionDate FROM StudySession s ORDER BY s.sessionDate DESC")
    List<LocalDate> findAllDistinctDatesOrderedDesc();
}
