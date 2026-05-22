package com.theology.tracker.service;

import com.theology.tracker.dto.CourseFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import com.theology.tracker.model.Topic;
import com.theology.tracker.model.WorkItemStatus;
import com.theology.tracker.repository.CourseRepository;
import com.theology.tracker.repository.TopicRepository;
import com.theology.tracker.repository.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepo;
    private final WorkItemRepository workItemRepo;
    private final TopicRepository topicRepo;

    public CourseService(CourseRepository courseRepo, WorkItemRepository workItemRepo, TopicRepository topicRepo) {
        this.courseRepo = courseRepo;
        this.workItemRepo = workItemRepo;
        this.topicRepo = topicRepo;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Course findById(Long id) {
        return courseRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
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

    public Course changeStatus(Long id, String newStatus) {
        Course course = findById(id);
        CourseStatus status = CourseStatus.valueOf(newStatus.toUpperCase());
        course.setStatus(status);
        if (status == CourseStatus.COMPLETE && course.getActualCompletion() == null) {
            course.setActualCompletion(LocalDate.now());
        } else if (status != CourseStatus.COMPLETE) {
            course.setActualCompletion(null);
        }
        return courseRepo.save(course);
    }

    @Transactional(readOnly = true)
    public List<Course> findTaggedByTopic(Long topicId) {
        return courseRepo.findByTopicId(topicId);
    }

    public void tagWithTopic(Long courseId, Long topicId) {
        Course course = findById(courseId);
        Topic topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        course.getTopics().add(topic);
        courseRepo.save(course);
    }

    public void untagFromTopic(Long courseId, Long topicId) {
        Course course = findById(courseId);
        course.getTopics().removeIf(t -> t.getId().equals(topicId));
        courseRepo.save(course);
    }

    @Transactional(readOnly = true)
    public int calculateProgress(Course course) {
        long total = course.getUnits().stream()
            .mapToLong(u -> workItemRepo.countByUnitId(u.getId()))
            .sum();
        if (total == 0) return 0;
        long completed = course.getUnits().stream()
            .mapToLong(u -> workItemRepo.countByUnitIdAndStatus(u.getId(), WorkItemStatus.COMPLETE))
            .sum();
        return (int) Math.round((completed * 100.0) / total);
    }

    private void applyForm(Course course, CourseFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Course title is required.");
        }
        course.setTitle(form.title().trim());
        course.setDescription(form.description() != null && !form.description().isBlank()
            ? form.description().trim() : null);
        if (form.status() != null && !form.status().isBlank()) {
            CourseStatus status = CourseStatus.valueOf(form.status().toUpperCase());
            course.setStatus(status);
            if (status == CourseStatus.COMPLETE && course.getActualCompletion() == null) {
                course.setActualCompletion(LocalDate.now());
            } else if (status != CourseStatus.COMPLETE) {
                course.setActualCompletion(null);
            }
        }
        course.setStartDate(form.startDate());
        course.setTargetCompletion(form.targetCompletion());
        if (form.topicIds() != null && !form.topicIds().isEmpty()) {
            course.setTopics(new HashSet<>(topicRepo.findAllById(form.topicIds())));
        } else {
            course.setTopics(new HashSet<>());
        }
    }
}
