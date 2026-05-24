package com.theology.tracker.controller;

import com.theology.tracker.dto.CourseFormDto;
import com.theology.tracker.dto.TaskFormDto;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import com.theology.tracker.model.Task;
import com.theology.tracker.model.TaskStatus;
import com.theology.tracker.service.CourseService;
import com.theology.tracker.service.NoteService;
import com.theology.tracker.service.PaperService;
import com.theology.tracker.service.TaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final PaperService paperService;

    public CourseController(CourseService courseService, TaskService taskService,
                            NoteService noteService, PaperService paperService) {
        this.courseService = courseService;
        this.taskService = taskService;
        this.noteService = noteService;
        this.paperService = paperService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("courses", courseService.findAll());
        model.addAttribute("statuses", CourseStatus.values());
        return "courses/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("pageTitle", "New Course");
        model.addAttribute("formAction", "/courses");
        return "courses/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "ACTIVE") String status,
        RedirectAttributes ra
    ) {
        Course course = courseService.create(new CourseFormDto(title, description, status));
        ra.addFlashAttribute("successMessage", "Course created.");
        return "redirect:/courses/" + course.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        List<Task> tasks = taskService.findByCourse(id);
        List<Task> toDo = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TO_DO).toList();
        List<Task> inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).toList();
        List<Task> done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).toList();

        model.addAttribute("course", course);
        model.addAttribute("toDo", toDo);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("done", done);
        model.addAttribute("allNotes", noteService.findAll());
        model.addAttribute("allPapers", paperService.findAll());
        model.addAttribute("statuses", CourseStatus.values());
        return "courses/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("course", courseService.findById(id));
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("pageTitle", "Edit Course");
        model.addAttribute("formAction", "/courses/" + id);
        return "courses/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "ACTIVE") String status,
        RedirectAttributes ra
    ) {
        courseService.update(id, new CourseFormDto(title, description, status));
        ra.addFlashAttribute("successMessage", "Course updated.");
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        courseService.delete(id);
        ra.addFlashAttribute("successMessage", "Course deleted.");
        return "redirect:/courses";
    }

    // --- Task CRUD ---

    @PostMapping("/{courseId}/tasks")
    public String createTask(
        @PathVariable Long courseId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "TO_DO") String status,
        @RequestParam(required = false) String dueDate,
        RedirectAttributes ra
    ) {
        LocalDate due = dueDate != null && !dueDate.isBlank() ? LocalDate.parse(dueDate) : null;
        taskService.create(courseId, new TaskFormDto(title, description, status, due));
        ra.addFlashAttribute("successMessage", "Task added.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/tasks/{taskId}")
    public String updateTask(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "TO_DO") String status,
        @RequestParam(required = false) String dueDate,
        RedirectAttributes ra
    ) {
        LocalDate due = dueDate != null && !dueDate.isBlank() ? LocalDate.parse(dueDate) : null;
        taskService.update(taskId, new TaskFormDto(title, description, status, due));
        ra.addFlashAttribute("successMessage", "Task updated.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/tasks/{taskId}/status")
    @ResponseBody
    public String updateTaskStatus(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @RequestParam String status
    ) {
        taskService.updateStatus(taskId, status);
        return "ok";
    }

    @PostMapping(value = "/{courseId}/tasks/reorder", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public String reorderTasks(
        @PathVariable Long courseId,
        @RequestParam("ids[]") List<Long> ids
    ) {
        taskService.reorder(courseId, ids);
        return "ok";
    }

    @PostMapping("/{courseId}/tasks/{taskId}/delete")
    public String deleteTask(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        RedirectAttributes ra
    ) {
        taskService.delete(taskId);
        ra.addFlashAttribute("successMessage", "Task deleted.");
        return "redirect:/courses/" + courseId;
    }

    // --- Task attachments ---

    @PostMapping("/{courseId}/tasks/{taskId}/notes/attach")
    public String attachNote(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @RequestParam Long noteId,
        RedirectAttributes ra
    ) {
        taskService.attachNote(taskId, noteId);
        ra.addFlashAttribute("successMessage", "Note attached.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/tasks/{taskId}/notes/{noteId}/detach")
    public String detachNote(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @PathVariable Long noteId,
        RedirectAttributes ra
    ) {
        taskService.detachNote(taskId, noteId);
        ra.addFlashAttribute("successMessage", "Note removed.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/tasks/{taskId}/papers/attach")
    public String attachPaper(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @RequestParam Long paperId,
        RedirectAttributes ra
    ) {
        taskService.attachPaper(taskId, paperId);
        ra.addFlashAttribute("successMessage", "Paper attached.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{courseId}/tasks/{taskId}/papers/{paperId}/detach")
    public String detachPaper(
        @PathVariable Long courseId,
        @PathVariable Long taskId,
        @PathVariable Long paperId,
        RedirectAttributes ra
    ) {
        taskService.detachPaper(taskId, paperId);
        ra.addFlashAttribute("successMessage", "Paper removed.");
        return "redirect:/courses/" + courseId;
    }
}
