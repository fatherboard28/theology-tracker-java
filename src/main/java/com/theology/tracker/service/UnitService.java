package com.theology.tracker.service;

import com.theology.tracker.dto.UnitFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.Unit;
import com.theology.tracker.model.WorkItemStatus;
import com.theology.tracker.repository.CourseRepository;
import com.theology.tracker.repository.UnitRepository;
import com.theology.tracker.repository.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class UnitService {

    private final UnitRepository unitRepo;
    private final CourseRepository courseRepo;
    private final WorkItemRepository workItemRepo;

    public UnitService(UnitRepository unitRepo, CourseRepository courseRepo, WorkItemRepository workItemRepo) {
        this.unitRepo = unitRepo;
        this.courseRepo = courseRepo;
        this.workItemRepo = workItemRepo;
    }

    @Transactional(readOnly = true)
    public List<Unit> findByCourse(Long courseId) {
        return unitRepo.findByCourseIdOrderByUnitOrderAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Unit findById(Long id) {
        return unitRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + id));
    }

    public Unit create(Long courseId, UnitFormDto form) {
        Course course = courseRepo.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        List<Unit> existing = unitRepo.findByCourseIdOrderByUnitOrderAsc(courseId);
        int nextOrder = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getUnitOrder() + 1;
        Unit unit = new Unit();
        unit.setCourse(course);
        unit.setUnitOrder(nextOrder);
        applyForm(unit, form);
        return unitRepo.save(unit);
    }

    public Unit update(Long unitId, UnitFormDto form) {
        Unit unit = findById(unitId);
        applyForm(unit, form);
        return unitRepo.save(unit);
    }

    public void delete(Long unitId) {
        unitRepo.delete(findById(unitId));
    }

    public void reorder(Long courseId, List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Long unitId = orderedIds.get(i);
            Unit unit = unitRepo.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            unit.setUnitOrder(i + 1);
            unitRepo.save(unit);
        }
    }

    public void checkAndAutoComplete(Long unitId) {
        Unit unit = findById(unitId);
        long total = workItemRepo.countByUnitId(unitId);
        if (total == 0) return;
        long completed = workItemRepo.countByUnitIdAndStatus(unitId, WorkItemStatus.COMPLETE);
        if (total == completed && unit.getActualCompletion() == null) {
            unit.setActualCompletion(LocalDate.now());
            unitRepo.save(unit);
        } else if (total != completed && unit.getActualCompletion() != null) {
            unit.setActualCompletion(null);
            unitRepo.save(unit);
        }
    }

    private void applyForm(Unit unit, UnitFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Unit title is required.");
        }
        unit.setTitle(form.title().trim());
        unit.setDescription(form.description() != null && !form.description().isBlank()
            ? form.description().trim() : null);
        unit.setTargetCompletion(form.targetCompletion());
    }
}
