package com.theology.tracker.repository;

import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCreatedAtDesc();

    List<Course> findByStatusOrderByCreatedAtDesc(CourseStatus status);
}
