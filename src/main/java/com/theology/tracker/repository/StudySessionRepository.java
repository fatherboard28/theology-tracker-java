package com.theology.tracker.repository;

import com.theology.tracker.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s WHERE s.workItem.id = :workItemId")
    int sumDurationByWorkItemId(@Param("workItemId") Long workItemId);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s WHERE s.workItem.unit.id = :unitId")
    int sumDurationByUnitId(@Param("unitId") Long unitId);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s WHERE s.workItem.unit.course.id = :courseId")
    int sumDurationByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s JOIN s.topics t WHERE t.id = :topicId")
    int sumDurationByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT s FROM StudySession s ORDER BY s.sessionDate DESC LIMIT 1")
    Optional<StudySession> findLastSession();

    @Query("SELECT s FROM StudySession s WHERE s.reflectionNote IS NOT NULL AND LOWER(s.reflectionNote) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY s.sessionDate DESC")
    List<StudySession> searchByReflectionNote(@Param("q") String q);
}
