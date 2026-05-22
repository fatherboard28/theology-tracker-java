package com.theology.tracker.repository;

import com.theology.tracker.model.Topic;
import com.theology.tracker.model.TopicType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByParentTopicIsNullOrderByTitleAsc();

    List<Topic> findByParentTopicIdOrderByTitleAsc(Long parentTopicId);

    List<Topic> findByType(TopicType type);

    List<Topic> findByTitleContainingIgnoreCase(String titleFragment);

    List<Topic> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Topic t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY t.title")
    List<Topic> searchByTitleOrDescription(@Param("q") String q);
}
