package com.theology.tracker.service;

import com.theology.tracker.dto.CourseFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import com.theology.tracker.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepo;

    public CourseService(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @Transactional(readOnly = true)
    public Course findById(Long id) {
        return courseRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Course> findActive() {
        return courseRepo.findByStatusOrderByCreatedAtDesc(CourseStatus.ACTIVE);
    }

    public Course create(CourseFormDto form) {
        Course course = new Course();
        applyForm(course, form);
        return courseRepo.save(course);
    }

    public Course update(Long id, CourseFormDto form) {
        Course course = findById(id);
        applyForm(course, form);
        return courseRepo.save(course);
    }

    public void delete(Long id) {
        courseRepo.delete(findById(id));
    }

    private void applyForm(Course course, CourseFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Course title is required.");
        }
        course.setTitle(form.title().trim());
        course.setDescription(form.description() != null && !form.description().isBlank()
            ? form.description().trim() : null);
        if (form.status() != null && !form.status().isBlank()) {
            course.setStatus(CourseStatus.valueOf(form.status().toUpperCase()));
        }
    }
}
