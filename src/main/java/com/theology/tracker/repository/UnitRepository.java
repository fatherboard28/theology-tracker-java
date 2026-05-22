package com.theology.tracker.repository;

import com.theology.tracker.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByCourseIdOrderByUnitOrderAsc(Long courseId);

    void deleteByCourseId(Long courseId);

    List<Unit> findByTargetCompletionBetweenAndActualCompletionIsNull(LocalDate from, LocalDate to);

    List<Unit> findByTargetCompletionBetween(LocalDate from, LocalDate to);

    @Query("SELECT u FROM Unit u JOIN u.topics t WHERE t.id = :topicId ORDER BY u.title")
    List<Unit> findByTopicId(@Param("topicId") Long topicId);
}
