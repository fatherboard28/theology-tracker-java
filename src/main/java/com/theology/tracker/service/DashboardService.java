package com.theology.tracker.service;

import com.theology.tracker.model.Course;
import com.theology.tracker.model.Note;
import com.theology.tracker.model.Paper;
import com.theology.tracker.model.TaskStatus;
import com.theology.tracker.repository.CourseRepository;
import com.theology.tracker.repository.NoteRepository;
import com.theology.tracker.repository.PaperRepository;
import com.theology.tracker.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CourseRepository courseRepo;
    private final NoteRepository noteRepo;
    private final PaperRepository paperRepo;
    private final TaskRepository taskRepo;

    public DashboardService(CourseRepository courseRepo, NoteRepository noteRepo,
                            PaperRepository paperRepo, TaskRepository taskRepo) {
        this.courseRepo = courseRepo;
        this.noteRepo = noteRepo;
        this.paperRepo = paperRepo;
        this.taskRepo = taskRepo;
    }

    public record CourseProgress(Course course, long total, long done) {}

    public List<CourseProgress> getActiveCoursesWithProgress() {
        return courseRepo.findAllByOrderByCreatedAtDesc().stream()
            .filter(c -> c.getStatus().name().equals("ACTIVE"))
            .map(c -> {
                long total = taskRepo.findByCourseIdOrderByBoardPositionAsc(c.getId()).size();
                long done = taskRepo.findByCourseIdAndStatusOrderByBoardPositionAsc(c.getId(), TaskStatus.DONE).size();
                return new CourseProgress(c, total, done);
            })
            .toList();
    }

    public List<Note> getRecentNotes() {
        List<Note> all = noteRepo.findAllByOrderByUpdatedAtDesc();
        return all.size() > 8 ? all.subList(0, 8) : all;
    }

    public List<Paper> getRecentPapers() {
        List<Paper> all = paperRepo.findAllByOrderByUpdatedAtDesc();
        return all.size() > 8 ? all.subList(0, 8) : all;
    }
}
