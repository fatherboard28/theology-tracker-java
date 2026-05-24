package com.theology.tracker.service;

import com.theology.tracker.dto.TaskFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.*;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.PaperRepository;
import com.theology.tracker.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepo;
    private final CourseService courseService;
    private final NoteRepository noteRepo;
    private final PaperRepository paperRepo;

    public TaskService(TaskRepository taskRepo, CourseService courseService,
                       NoteRepository noteRepo, PaperRepository paperRepo) {
        this.taskRepo = taskRepo;
        this.courseService = courseService;
        this.noteRepo = noteRepo;
        this.paperRepo = paperRepo;
    }

    @Transactional(readOnly = true)
    public Task findById(Long id) {
        return taskRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Task> findByCourse(Long courseId) {
        return taskRepo.findByCourseIdOrderByBoardPositionAsc(courseId);
    }

    public Task create(Long courseId, TaskFormDto form) {
        Course course = courseService.findById(courseId);
        Task task = new Task();
        task.setCourse(course);
        applyForm(task, form);
        long count = taskRepo.findByCourseIdOrderByBoardPositionAsc(courseId).size();
        task.setBoardPosition((int) count);
        return taskRepo.save(task);
    }

    public Task update(Long id, TaskFormDto form) {
        Task task = findById(id);
        applyForm(task, form);
        return taskRepo.save(task);
    }

    public void updateStatus(Long id, String status) {
        Task task = findById(id);
        task.setStatus(TaskStatus.valueOf(status.toUpperCase()));
        taskRepo.save(task);
    }

    public void reorder(Long courseId, List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Task task = taskRepo.findById(orderedIds.get(i))
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
            task.setBoardPosition(i);
            taskRepo.save(task);
        }
    }

    public void attachNote(Long taskId, Long noteId) {
        Task task = findById(taskId);
        Note note = noteRepo.findById(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));
        task.getNoteRefs().add(note);
        taskRepo.save(task);
    }

    public void detachNote(Long taskId, Long noteId) {
        Task task = findById(taskId);
        task.getNoteRefs().removeIf(n -> n.getId().equals(noteId));
        taskRepo.save(task);
    }

    public void attachPaper(Long taskId, Long paperId) {
        Task task = findById(taskId);
        Paper paper = paperRepo.findById(paperId)
            .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));
        task.getPaperRefs().add(paper);
        taskRepo.save(task);
    }

    public void detachPaper(Long taskId, Long paperId) {
        Task task = findById(taskId);
        task.getPaperRefs().removeIf(p -> p.getId().equals(paperId));
        taskRepo.save(task);
    }

    public void delete(Long id) {
        taskRepo.delete(findById(id));
    }

    private void applyForm(Task task, TaskFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }
        task.setTitle(form.title().trim());
        task.setDescription(form.description() != null && !form.description().isBlank()
            ? form.description().trim() : null);
        if (form.status() != null && !form.status().isBlank()) {
            task.setStatus(TaskStatus.valueOf(form.status().toUpperCase()));
        }
        task.setDueDate(form.dueDate());
    }
}
