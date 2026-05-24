package com.theology.tracker.repository;

import com.theology.tracker.model.Task;
import com.theology.tracker.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCourseIdOrderByBoardPositionAsc(Long courseId);

    List<Task> findByCourseIdAndStatusOrderByBoardPositionAsc(Long courseId, TaskStatus status);
}
