package com.theology.tracker.repository;

import com.theology.tracker.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByCourseIdOrderByUnitOrderAsc(Long courseId);

    void deleteByCourseId(Long courseId);
}
