package com.theology.tracker.repository;

import com.theology.tracker.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findAllByOrderByUpdatedAtDesc();

    List<Note> findByStarredTrueOrderByUpdatedAtDesc();

    @Query("SELECT n FROM Note n JOIN n.topics t WHERE t.id = :topicId ORDER BY n.updatedAt DESC")
    List<Note> findByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT n FROM Note n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(n.body) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY n.updatedAt DESC")
    List<Note> searchByTitleOrBody(@Param("q") String q);

    @Query(value = "SELECT * FROM notes WHERE body LIKE '%[[' || :title || ']]%' ORDER BY updated_at DESC", nativeQuery = true)
    List<Note> findBacklinks(@Param("title") String noteTitle);
}
