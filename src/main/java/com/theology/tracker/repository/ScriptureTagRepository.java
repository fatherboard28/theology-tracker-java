package com.theology.tracker.repository;

import com.theology.tracker.model.ScriptureEntityType;
import com.theology.tracker.model.ScriptureTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptureTagRepository extends JpaRepository<ScriptureTag, Long> {

    List<ScriptureTag> findByEntityTypeAndEntityId(ScriptureEntityType entityType, Long entityId);

    List<ScriptureTag> findByReference(String reference);

    List<ScriptureTag> findByReferenceStartingWith(String referencePrefix);

    void deleteByEntityTypeAndEntityId(ScriptureEntityType entityType, Long entityId);
}
