package com.theology.tracker.repository;

import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    List<Course> findAllByOrderByCreatedAtDesc();

    List<Course> findByTargetCompletionBetweenAndStatusNot(LocalDate from, LocalDate to, CourseStatus status);

    List<Course> findByTargetCompletionBetween(LocalDate from, LocalDate to);
}
