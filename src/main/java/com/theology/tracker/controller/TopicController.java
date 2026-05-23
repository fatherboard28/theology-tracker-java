package com.theology.tracker.controller;

import com.theology.tracker.dto.TopicFormDto;
import com.theology.tracker.model.*;
import com.theology.tracker.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;
    private final WorkItemService workItemService;
    private final NoteService noteService;
    private final StudySessionService sessionService;
    private final ProgressService progressService;
    private final CourseService courseService;
    private final UnitService unitService;

    public TopicController(
        TopicService topicService,
        WorkItemService workItemService,
        NoteService noteService,
        StudySessionService sessionService,
        ProgressService progressService,
        CourseService courseService,
        UnitService unitService
    ) {
        this.topicService = topicService;
        this.workItemService = workItemService;
        this.noteService = noteService;
        this.sessionService = sessionService;
        this.progressService = progressService;
        this.courseService = courseService;
        this.unitService = unitService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rootTopics", topicService.findRoots());
        return "topics/index";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long parentTopicId, Model model) {
        Topic topic = new Topic();
        if (parentTopicId != null) {
            Topic parent = topicService.findById(parentTopicId);
            topic.setParentTopic(parent);
        }
        model.addAttribute("topic", topic);
        model.addAttribute("types", TopicType.values());
        model.addAttribute("rootTopics", topicService.findRoots());
        model.addAttribute("formAction", "/topics");
        model.addAttribute("pageTitle", "New Topic");
        return "topics/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "OTHER") String type,
        @RequestParam(required = false) Long parentTopicId,
        RedirectAttributes ra
    ) {
        Topic topic = topicService.create(new TopicFormDto(title, description, type, parentTopicId));
        ra.addFlashAttribute("successMessage", "Topic \"" + topic.getTitle() + "\" created.");
        return "redirect:/topics/" + topic.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Topic topic = topicService.findById(id);

        List<Topic> rootTopics = topicService.findRoots().stream()
            .filter(t -> !t.getId().equals(id))
            .toList();

        List<WorkItem> ownedWorkItems = workItemService.findByOwningTopic(id);
        List<Note> ownedNotes = noteService.findByParent(NoteParentType.TOPIC, id);
        List<WorkItem> taggedWorkItems = workItemService.findTaggedByTopic(id);
        List<Course> taggedCourses = courseService.findTaggedByTopic(id);
        List<Unit> taggedUnits = unitService.findTaggedByTopic(id);
        List<Note> taggedNotes = noteService.findTaggedByTopic(id);
        List<StudySession> sessions = sessionService.findByTopic(id);

        // Group owned work items by type
        List<WorkItem> ownedReadings = ownedWorkItems.stream().filter(w -> w instanceof Reading).toList();
        List<WorkItem> ownedAssignments = ownedWorkItems.stream().filter(w -> w instanceof Assignment).toList();
        List<WorkItem> ownedPapers = ownedWorkItems.stream().filter(w -> w instanceof Paper).toList();
        List<WorkItem> ownedPractices = ownedWorkItems.stream().filter(w -> w instanceof PracticeSessionItem).toList();

        // Summary: union of owned and tagged (avoid double-counting items that appear in both)
        Set<Long> ownedIds = ownedWorkItems.stream().map(WorkItem::getId).collect(Collectors.toSet());
        List<WorkItem> taggedOnly = taggedWorkItems.stream()
            .filter(w -> !ownedIds.contains(w.getId()))
            .toList();
        long totalWorkItems = ownedWorkItems.size() + taggedOnly.size();
        long completeWorkItems = ownedWorkItems.stream().filter(w -> w.getStatus() == WorkItemStatus.COMPLETE).count()
            + taggedOnly.stream().filter(w -> w.getStatus() == WorkItemStatus.COMPLETE).count();

        // Available entities for tagging (not already tagged or owned by this topic)
        Set<Long> taggedCourseIds = taggedCourses.stream().map(Course::getId).collect(Collectors.toSet());
        Set<Long> taggedUnitIds = taggedUnits.stream().map(Unit::getId).collect(Collectors.toSet());
        Set<Long> alreadyAssocWorkItemIds = Stream.concat(ownedWorkItems.stream(), taggedWorkItems.stream())
            .map(WorkItem::getId).collect(Collectors.toSet());
        Set<Long> ownedNoteIds = ownedNotes.stream().map(Note::getId).collect(Collectors.toSet());
        List<Note> uniqueTaggedNotes = taggedNotes.stream()
            .filter(n -> !ownedNoteIds.contains(n.getId()))
            .toList();
        Set<Long> taggedOnlyNoteIds = uniqueTaggedNotes.stream().map(Note::getId).collect(Collectors.toSet());
        List<Note> allNotes = new ArrayList<>();
        allNotes.addAll(ownedNotes);
        allNotes.addAll(uniqueTaggedNotes);
        allNotes.sort(Comparator.comparing(Note::isStarred).reversed()
            .thenComparing(Comparator.comparing(Note::getUpdatedAt).reversed()));
        Set<Long> alreadyAssocNoteIds = Stream.concat(ownedNoteIds.stream(), taggedOnlyNoteIds.stream())
            .collect(Collectors.toSet());

        List<Course> availableCourses = courseService.findAll().stream()
            .filter(c -> !taggedCourseIds.contains(c.getId()))
            .toList();
        List<Unit> availableUnits = unitService.findAll().stream()
            .filter(u -> !taggedUnitIds.contains(u.getId()))
            .toList();
        List<WorkItem> availableWorkItems = workItemService.findAll().stream()
            .filter(w -> !alreadyAssocWorkItemIds.contains(w.getId()))
            .toList();
        List<Note> availableNotes = noteService.findAll().stream()
            .filter(n -> !alreadyAssocNoteIds.contains(n.getId()))
            .toList();

        model.addAttribute("topic", topic);
        model.addAttribute("rootTopics", rootTopics);
        model.addAttribute("ownedWorkItems", ownedWorkItems);
        model.addAttribute("ownedReadings", ownedReadings);
        model.addAttribute("ownedAssignments", ownedAssignments);
        model.addAttribute("ownedPapers", ownedPapers);
        model.addAttribute("ownedPractices", ownedPractices);
        model.addAttribute("allNotes", allNotes);
        model.addAttribute("taggedOnlyNoteIds", taggedOnlyNoteIds);
        model.addAttribute("taggedWorkItems", taggedWorkItems);
        model.addAttribute("taggedCourses", taggedCourses);
        model.addAttribute("taggedUnits", taggedUnits);
        model.addAttribute("sessions", sessions);
        model.addAttribute("totalWorkItems", totalWorkItems);
        model.addAttribute("completeWorkItems", completeWorkItems);
        model.addAttribute("totalLoggedMinutes", progressService.totalMinutesForTopic(id));
        model.addAttribute("availableCourses", availableCourses);
        model.addAttribute("availableUnits", availableUnits);
        model.addAttribute("availableWorkItems", availableWorkItems);
        model.addAttribute("availableNotes", availableNotes);
        return "topics/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Topic topic = topicService.findById(id);
        List<Topic> rootTopics = topicService.findRoots().stream()
            .filter(t -> !t.getId().equals(id))
            .toList();
        model.addAttribute("topic", topic);
        model.addAttribute("types", TopicType.values());
        model.addAttribute("rootTopics", rootTopics);
        model.addAttribute("formAction", "/topics/" + id);
        model.addAttribute("pageTitle", "Edit Topic");
        return "topics/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "OTHER") String type,
        @RequestParam(required = false) Long parentTopicId,
        RedirectAttributes ra
    ) {
        topicService.update(id, new TopicFormDto(title, description, type, parentTopicId));
        ra.addFlashAttribute("successMessage", "Topic updated.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        topicService.delete(id);
        ra.addFlashAttribute("successMessage", "Topic deleted.");
        return "redirect:/topics";
    }

    // --- Tagging actions ---

    @PostMapping("/{id}/tag-course")
    public String tagCourse(@PathVariable Long id, @RequestParam Long courseId, RedirectAttributes ra) {
        courseService.tagWithTopic(courseId, id);
        ra.addFlashAttribute("successMessage", "Course tagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/untag-course/{courseId}")
    public String untagCourse(@PathVariable Long id, @PathVariable Long courseId, RedirectAttributes ra) {
        courseService.untagFromTopic(courseId, id);
        ra.addFlashAttribute("successMessage", "Course untagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/tag-unit")
    public String tagUnit(@PathVariable Long id, @RequestParam Long unitId, RedirectAttributes ra) {
        unitService.tagWithTopic(unitId, id);
        ra.addFlashAttribute("successMessage", "Unit tagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/untag-unit/{unitId}")
    public String untagUnit(@PathVariable Long id, @PathVariable Long unitId, RedirectAttributes ra) {
        unitService.untagFromTopic(unitId, id);
        ra.addFlashAttribute("successMessage", "Unit untagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/tag-work-item")
    public String tagWorkItem(@PathVariable Long id, @RequestParam Long workItemId, RedirectAttributes ra) {
        workItemService.tagWithTopic(workItemId, id);
        ra.addFlashAttribute("successMessage", "Work item tagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/untag-work-item/{workItemId}")
    public String untagWorkItem(@PathVariable Long id, @PathVariable Long workItemId, RedirectAttributes ra) {
        workItemService.untagFromTopic(workItemId, id);
        ra.addFlashAttribute("successMessage", "Work item untagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/tag-note")
    public String tagNote(@PathVariable Long id, @RequestParam Long noteId, RedirectAttributes ra) {
        noteService.tagWithTopic(noteId, id);
        ra.addFlashAttribute("successMessage", "Note tagged.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/untag-note/{noteId}")
    public String untagNote(@PathVariable Long id, @PathVariable Long noteId, RedirectAttributes ra) {
        noteService.untagFromTopic(noteId, id);
        ra.addFlashAttribute("successMessage", "Note untagged.");
        return "redirect:/topics/" + id;
    }
}
