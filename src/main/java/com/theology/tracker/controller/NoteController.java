package com.theology.tracker.controller;

import com.theology.tracker.dto.NoteFormDto;
import com.theology.tracker.model.Note;
import com.theology.tracker.model.NoteParentType;
import com.theology.tracker.model.WorkItem;
import com.theology.tracker.service.NoteService;
import com.theology.tracker.service.NoteService.ParentInfo;
import com.theology.tracker.service.TopicService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NoteController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final NoteService noteService;
    private final TopicService topicService;

    public NoteController(NoteService noteService, TopicService topicService) {
        this.noteService = noteService;
        this.topicService = topicService;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false) String parentType,
        @RequestParam(required = false) Long parentId,
        Model model
    ) {
        if (parentType != null && parentId != null) {
            NoteParentType type = NoteParentType.valueOf(parentType.toUpperCase());
            model.addAttribute("notes", noteService.findByParent(type, parentId));
            model.addAttribute("filterParent", noteService.resolveParent(type, parentId));
        } else {
            model.addAttribute("notes", noteService.findAll());
        }
        return "notes/index";
    }

    @GetMapping("/new")
    public String newForm(
        @RequestParam String parentType,
        @RequestParam Long parentId,
        Model model
    ) {
        NoteParentType type = NoteParentType.valueOf(parentType.toUpperCase());
        model.addAttribute("parent", noteService.resolveParent(type, parentId));
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("pageTitle", "New Note");
        model.addAttribute("formAction", "/notes");
        return "notes/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(defaultValue = "") String body,
        @RequestParam String parentType,
        @RequestParam Long parentId,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        Note note = noteService.create(new NoteFormDto(title, body, parentType, parentId, topicIds));
        ra.addFlashAttribute("successMessage", "Note created.");
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Note note = noteService.findById(id);
        ParentInfo parent = noteService.resolveParent(note.getPrimaryParentType(), note.getPrimaryParentId());
        List<WorkItem> availableRefs = noteService.findReferenceableWorkItems().stream()
            .filter(wi -> note.getWorkItemRefs().stream().noneMatch(r -> r.getId().equals(wi.getId())))
            .toList();
        model.addAttribute("note", note);
        model.addAttribute("parent", parent);
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("availableWorkItems", availableRefs);
        return "notes/show";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(defaultValue = "") String body,
        @RequestParam String parentType,
        @RequestParam Long parentId,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        noteService.update(id, new NoteFormDto(title, body, parentType, parentId, topicIds));
        ra.addFlashAttribute("successMessage", "Note saved.");
        return "redirect:/notes/" + id;
    }

    @PostMapping(value = "/{id}/autosave", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String autosave(@PathVariable Long id, @RequestParam(defaultValue = "") String body) {
        LocalDateTime savedAt = noteService.autoSave(id, body);
        return "<span class=\"text-muted text-small\" style=\"font-style:italic;\">Saved at "
            + savedAt.format(TIME_FMT) + "</span>";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Note note = noteService.findById(id);
        ParentInfo parent = noteService.resolveParent(note.getPrimaryParentType(), note.getPrimaryParentId());
        noteService.delete(id);
        ra.addFlashAttribute("successMessage", "Note deleted.");
        return "redirect:" + parent.url();
    }

    @PostMapping("/{id}/work-items/attach")
    public String attachWorkItem(
        @PathVariable Long id,
        @RequestParam Long workItemId,
        RedirectAttributes ra
    ) {
        noteService.attachWorkItemRef(id, workItemId);
        ra.addFlashAttribute("successMessage", "Work item referenced.");
        return "redirect:/notes/" + id;
    }

    @PostMapping("/{id}/work-items/{wiId}/detach")
    public String detachWorkItem(
        @PathVariable Long id,
        @PathVariable Long wiId,
        RedirectAttributes ra
    ) {
        noteService.detachWorkItemRef(id, wiId);
        ra.addFlashAttribute("successMessage", "Reference removed.");
        return "redirect:/notes/" + id;
    }
}
