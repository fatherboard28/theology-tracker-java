package com.theology.tracker.repository;

import com.theology.tracker.model.Paper;
import com.theology.tracker.model.PaperStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findAllByOrderByUpdatedAtDesc();

    List<Paper> findByStatusOrderByUpdatedAtDesc(PaperStatus status);

    @Query("SELECT p FROM Paper p JOIN p.topics t WHERE t.id = :topicId ORDER BY p.updatedAt DESC")
    List<Paper> findByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT p FROM Paper p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.thesis) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY p.updatedAt DESC")
    List<Paper> searchByTitleOrThesis(@Param("q") String q);
}
