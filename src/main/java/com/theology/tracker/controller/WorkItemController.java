package com.theology.tracker.controller;

import com.theology.tracker.dto.WorkItemFormDto;
import com.theology.tracker.model.*;
import com.theology.tracker.service.MethodService;
import com.theology.tracker.service.StudySessionService;
import com.theology.tracker.service.TopicService;
import com.theology.tracker.service.UnitService;
import com.theology.tracker.service.WorkItemService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/work-items")
public class WorkItemController {

    private final WorkItemService workItemService;
    private final TopicService topicService;
    private final UnitService unitService;
    private final MethodService methodService;
    private final StudySessionService sessionService;

    public WorkItemController(
        WorkItemService workItemService,
        TopicService topicService,
        UnitService unitService,
        MethodService methodService,
        StudySessionService sessionService
    ) {
        this.workItemService = workItemService;
        this.topicService = topicService;
        this.unitService = unitService;
        this.methodService = methodService;
        this.sessionService = sessionService;
    }

    @GetMapping("/new")
    public String newForm(
        @RequestParam String type,
        @RequestParam(required = false) Long unitId,
        @RequestParam(required = false) Long owningTopicId,
        Model model
    ) {
        WorkItemType workItemType = WorkItemType.valueOf(type.toUpperCase());
        model.addAttribute("formType", workItemType);
        model.addAttribute("unitId", unitId);
        model.addAttribute("owningTopicId", owningTopicId);
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("statuses", WorkItemStatus.values());
        model.addAttribute("readingFormats", ReadingFormat.values());
        model.addAttribute("allMethods", methodService.findAll());
        model.addAttribute("pageTitle", "New " + formatTypeName(workItemType));
        model.addAttribute("formAction", "/work-items");
        if (unitId != null) {
            Unit unit = unitService.findById(unitId);
            model.addAttribute("unit", unit);
            model.addAttribute("backUrl", "/courses/" + unit.getCourse().getId());
        } else if (owningTopicId != null) {
            model.addAttribute("owningTopic", topicService.findById(owningTopicId));
            model.addAttribute("backUrl", "/topics/" + owningTopicId);
        }
        return "work-items/form";
    }

    @PostMapping
    public String create(
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer estimatedDuration,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
        @RequestParam(required = false) String generalNotes,
        @RequestParam(required = false) Long unitId,
        @RequestParam(required = false) Long owningTopicId,
        @RequestParam(required = false) List<Long> topicIds,
        @RequestParam(required = false) List<String> scriptureTags,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String format,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String promptOrTopic,
        @RequestParam(required = false) Integer wordCountTarget,
        @RequestParam(required = false) String scoreOrGrade,
        @RequestParam(required = false) Long methodId,
        @RequestParam(required = false) String scripturePassage,
        @RequestParam(required = false) Integer durationMinutes,
        RedirectAttributes ra
    ) {
        WorkItemFormDto form = new WorkItemFormDto(
            title, type, status, estimatedDuration, dueDate, generalNotes,
            unitId, owningTopicId, topicIds, scriptureTags,
            source, author, location, format,
            description,
            promptOrTopic, wordCountTarget, scoreOrGrade,
            methodId, scripturePassage, durationMinutes
        );
        WorkItem item = workItemService.create(form);
        ra.addFlashAttribute("successMessage", "Work item created.");
        return "redirect:" + resolveRedirect(item);
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        WorkItem item = workItemService.findById(id);
        model.addAttribute("item", item);
        model.addAttribute("scriptureTags", workItemService.findScriptureTagsForWorkItem(id));
        model.addAttribute("sessions", sessionService.findByWorkItem(id));
        model.addAttribute("backUrl", resolveBackUrl(item));
        model.addAttribute("typeLabel", formatTypeName(resolveType(item)));
        addTypedSubAttribute(model, item);
        return "work-items/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        WorkItem item = workItemService.findById(id);
        WorkItemType workItemType = resolveType(item);
        model.addAttribute("item", item);
        model.addAttribute("formType", workItemType);
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("statuses", WorkItemStatus.values());
        model.addAttribute("readingFormats", ReadingFormat.values());
        model.addAttribute("allMethods", methodService.findAll());
        model.addAttribute("scriptureTags", workItemService.findScriptureTagsForWorkItem(id));
        model.addAttribute("pageTitle", "Edit " + formatTypeName(workItemType));
        model.addAttribute("formAction", "/work-items/" + id);
        model.addAttribute("backUrl", resolveBackUrl(item));
        addTypedSubAttribute(model, item);
        return "work-items/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String type,
        @RequestParam String title,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer estimatedDuration,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
        @RequestParam(required = false) String generalNotes,
        @RequestParam(required = false) Long unitId,
        @RequestParam(required = false) Long owningTopicId,
        @RequestParam(required = false) List<Long> topicIds,
        @RequestParam(required = false) List<String> scriptureTags,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String format,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String promptOrTopic,
        @RequestParam(required = false) Integer wordCountTarget,
        @RequestParam(required = false) String scoreOrGrade,
        @RequestParam(required = false) Long methodId,
        @RequestParam(required = false) String scripturePassage,
        @RequestParam(required = false) Integer durationMinutes,
        RedirectAttributes ra
    ) {
        WorkItemFormDto form = new WorkItemFormDto(
            title, type, status, estimatedDuration, dueDate, generalNotes,
            unitId, owningTopicId, topicIds, scriptureTags,
            source, author, location, format,
            description,
            promptOrTopic, wordCountTarget, scoreOrGrade,
            methodId, scripturePassage, durationMinutes
        );
        WorkItem item = workItemService.update(id, form);
        ra.addFlashAttribute("successMessage", "Work item updated.");
        return "redirect:/work-items/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        WorkItem item = workItemService.findById(id);
        String redirect = resolveBackUrl(item);
        workItemService.delete(id);
        ra.addFlashAttribute("successMessage", "Work item deleted.");
        return "redirect:" + redirect;
    }

    @PostMapping("/{id}/complete")
    public String toggleComplete(
        @PathVariable Long id,
        @RequestParam(required = false) String returnUrl,
        RedirectAttributes ra
    ) {
        WorkItem item = workItemService.toggleComplete(id);
        String msg = item.getStatus() == WorkItemStatus.COMPLETE
            ? "Marked complete." : "Marked incomplete.";
        ra.addFlashAttribute("successMessage", msg);
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:" + resolveBackUrl(item);
    }

    private String resolveRedirect(WorkItem item) {
        return "/work-items/" + item.getId();
    }

    private String resolveBackUrl(WorkItem item) {
        if (item.getUnit() != null) {
            return "/courses/" + item.getUnit().getCourse().getId();
        }
        if (item.getOwningTopic() != null) {
            return "/topics/" + item.getOwningTopic().getId();
        }
        return "/work-items";
    }

    private WorkItemType resolveType(WorkItem item) {
        if (item instanceof Reading) return WorkItemType.READING;
        if (item instanceof Assignment) return WorkItemType.ASSIGNMENT;
        if (item instanceof Paper) return WorkItemType.PAPER;
        if (item instanceof PracticeSessionItem) return WorkItemType.PRACTICE_SESSION;
        throw new IllegalStateException("Unknown work item type: " + item.getClass());
    }

    private String formatTypeName(WorkItemType type) {
        return switch (type) {
            case READING -> "Reading";
            case ASSIGNMENT -> "Assignment";
            case PAPER -> "Paper";
            case PRACTICE_SESSION -> "Practice Session";
        };
    }

    private void addTypedSubAttribute(Model model, WorkItem item) {
        if (item instanceof Reading r) model.addAttribute("reading", r);
        else if (item instanceof Assignment a) model.addAttribute("assignment", a);
        else if (item instanceof Paper p) model.addAttribute("paper", p);
        else if (item instanceof PracticeSessionItem ps) model.addAttribute("practiceSession", ps);
    }
}
