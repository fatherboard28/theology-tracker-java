package com.theology.tracker.controller;

import com.theology.tracker.dto.CourseFormDto;
import com.theology.tracker.model.Course;
import com.theology.tracker.model.CourseStatus;
import com.theology.tracker.model.NoteParentType;
import com.theology.tracker.service.CourseService;
import com.theology.tracker.service.NoteService;
import com.theology.tracker.service.TopicService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final TopicService topicService;
    private final NoteService noteService;

    public CourseController(CourseService courseService, TopicService topicService, NoteService noteService) {
        this.courseService = courseService;
        this.topicService = topicService;
        this.noteService = noteService;
    }

    @GetMapping
    public String list(Model model) {
        List<Course> courses = courseService.findAll();
        Map<Long, Integer> progressMap = new LinkedHashMap<>();
        courses.forEach(c -> progressMap.put(c.getId(), courseService.calculateProgress(c)));
        model.addAttribute("courses", courses);
        model.addAttribute("progressMap", progressMap);
        return "courses/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("formAction", "/courses");
        model.addAttribute("pageTitle", "New Course");
        return "courses/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "ACTIVE") String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetCompletion,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        CourseFormDto form = new CourseFormDto(title, description, status, startDate, targetCompletion, topicIds);
        Course course = courseService.create(form);
        ra.addFlashAttribute("successMessage", "Course \"" + course.getTitle() + "\" created.");
        return "redirect:/courses/" + course.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        model.addAttribute("course", course);
        model.addAttribute("progress", courseService.calculateProgress(course));
        model.addAttribute("units", course.getUnits());
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("notes", noteService.findByParent(NoteParentType.COURSE, id));
        return "courses/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        model.addAttribute("course", course);
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("formAction", "/courses/" + id);
        model.addAttribute("pageTitle", "Edit Course");
        return "courses/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "ACTIVE") String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetCompletion,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        CourseFormDto form = new CourseFormDto(title, description, status, startDate, targetCompletion, topicIds);
        courseService.update(id, form);
        ra.addFlashAttribute("successMessage", "Course updated.");
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        courseService.delete(id);
        ra.addFlashAttribute("successMessage", "Course deleted.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(
        @PathVariable Long id,
        @RequestParam String status,
        RedirectAttributes ra
    ) {
        courseService.changeStatus(id, status);
        ra.addFlashAttribute("successMessage", "Status updated.");
        return "redirect:/courses/" + id;
    }
}
