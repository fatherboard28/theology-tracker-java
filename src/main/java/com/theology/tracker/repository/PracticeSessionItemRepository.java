package com.theology.tracker.repository;

import com.theology.tracker.model.PracticeSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeSessionItemRepository extends JpaRepository<PracticeSessionItem, Long> {

    List<PracticeSessionItem> findByMethodId(Long methodId);
}
