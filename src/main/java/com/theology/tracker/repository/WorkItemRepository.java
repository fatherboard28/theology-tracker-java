package com.theology.tracker.repository;

import com.theology.tracker.model.WorkItem;
import com.theology.tracker.model.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    List<WorkItem> findAllByOrderByTitleAsc();

    List<WorkItem> findByUnitId(Long unitId);

    List<WorkItem> findByOwningTopicId(Long topicId);

    List<WorkItem> findByStatus(WorkItemStatus status);

    List<WorkItem> findByDueDateBetween(LocalDate from, LocalDate to);

    List<WorkItem> findByDueDateBeforeAndStatusNot(LocalDate date, WorkItemStatus status);

    @Query("SELECT w FROM WorkItem w WHERE TYPE(w) IN (Assignment, Paper, PracticeSessionItem) ORDER BY w.title")
    List<WorkItem> findReferenceableWorkItems();

    @Query("SELECT w FROM WorkItem w ORDER BY w.updatedAt DESC LIMIT 5")
    List<WorkItem> findTop5ByOrderByUpdatedAtDesc();

    long countByUnitId(Long unitId);

    long countByUnitIdAndStatus(Long unitId, WorkItemStatus status);
}
