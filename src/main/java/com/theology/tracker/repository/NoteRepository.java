package com.theology.tracker.repository;

import com.theology.tracker.model.Note;
import com.theology.tracker.model.NoteParentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByPrimaryParentTypeAndPrimaryParentIdOrderByUpdatedAtDesc(
        NoteParentType parentType, Long parentId);

    List<Note> findAllByOrderByUpdatedAtDesc();

    @Query("SELECT n FROM Note n ORDER BY n.updatedAt DESC LIMIT 5")
    List<Note> findTop5ByOrderByUpdatedAtDesc();

    @Query("SELECT n FROM Note n JOIN n.topicTags t WHERE t.id = :topicId ORDER BY n.updatedAt DESC")
    List<Note> findByTopicTagId(@Param("topicId") Long topicId);

    @Query("SELECT n FROM Note n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(n.body) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY n.updatedAt DESC")
    List<Note> searchByTitleOrBody(@Param("q") String q);
}
