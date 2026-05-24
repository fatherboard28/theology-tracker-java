package com.theology.tracker.controller;

import com.theology.tracker.dto.NoteFormDto;
import com.theology.tracker.model.Note;
import com.theology.tracker.service.NoteService;
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
    public String list(@RequestParam(required = false) Long topicId, Model model) {
        if (topicId != null) {
            model.addAttribute("notes", noteService.findByTopic(topicId));
            model.addAttribute("filterTopic", topicService.findById(topicId));
        } else {
            model.addAttribute("notes", noteService.findAll());
        }
        return "notes/index";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long topicId, Model model) {
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("preselectedTopicId", topicId);
        model.addAttribute("pageTitle", "New Note");
        model.addAttribute("formAction", "/notes");
        return "notes/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(defaultValue = "") String body,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        Note note = noteService.create(new NoteFormDto(title, body, topicIds));
        ra.addFlashAttribute("successMessage", "Note created.");
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Note note = noteService.findById(id);
        List<Note> backlinks = noteService.findBacklinks(note.getTitle());
        model.addAttribute("note", note);
        model.addAttribute("backlinks", backlinks);
        model.addAttribute("allTopics", topicService.findAll());
        return "notes/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Note note = noteService.findById(id);
        model.addAttribute("note", note);
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("pageTitle", "Edit Note");
        model.addAttribute("formAction", "/notes/" + id);
        return "notes/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(defaultValue = "") String body,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        noteService.update(id, new NoteFormDto(title, body, topicIds));
        ra.addFlashAttribute("successMessage", "Note saved.");
        return "redirect:/notes/" + id;
    }

    @PostMapping(value = "/{id}/autosave", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String autosave(@PathVariable Long id, @RequestParam(defaultValue = "") String body) {
        LocalDateTime savedAt = noteService.autoSave(id, body);
        return "<span style=\"font-style:italic;color:var(--text-secondary)\">Saved at "
            + savedAt.format(TIME_FMT) + "</span>";
    }

    @PostMapping("/{id}/star")
    public String toggleStar(@PathVariable Long id, RedirectAttributes ra) {
        noteService.toggleStar(id);
        return "redirect:/notes/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        noteService.delete(id);
        ra.addFlashAttribute("successMessage", "Note deleted.");
        return "redirect:/notes";
    }
}
