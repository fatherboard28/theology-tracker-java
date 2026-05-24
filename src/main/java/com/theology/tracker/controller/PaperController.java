package com.theology.tracker.controller;

import com.theology.tracker.dto.PaperFormDto;
import com.theology.tracker.model.Paper;
import com.theology.tracker.model.PaperStatus;
import com.theology.tracker.service.PaperService;
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
@RequestMapping("/papers")
public class PaperController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final PaperService paperService;
    private final TopicService topicService;

    public PaperController(PaperService paperService, TopicService topicService) {
        this.paperService = paperService;
        this.topicService = topicService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("papers", paperService.findAll());
        model.addAttribute("statuses", PaperStatus.values());
        return "papers/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("statuses", PaperStatus.values());
        model.addAttribute("pageTitle", "New Paper");
        model.addAttribute("formAction", "/papers");
        return "papers/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(required = false) String thesis,
        @RequestParam(required = false) String author,
        @RequestParam(defaultValue = "DRAFT") String status,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        Paper paper = paperService.create(new PaperFormDto(title, thesis, author, status, topicIds), author);
        ra.addFlashAttribute("successMessage", "Paper created.");
        return "redirect:/papers/" + paper.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Paper paper = paperService.findById(id);
        model.addAttribute("paper", paper);
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("statuses", PaperStatus.values());
        return "papers/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Paper paper = paperService.findById(id);
        model.addAttribute("paper", paper);
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("statuses", PaperStatus.values());
        model.addAttribute("pageTitle", "Edit Paper");
        model.addAttribute("formAction", "/papers/" + id);
        return "papers/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String thesis,
        @RequestParam(required = false) String author,
        @RequestParam(defaultValue = "DRAFT") String status,
        @RequestParam(required = false) List<Long> topicIds,
        RedirectAttributes ra
    ) {
        paperService.update(id, new PaperFormDto(title, thesis, author, status, topicIds));
        ra.addFlashAttribute("successMessage", "Paper updated.");
        return "redirect:/papers/" + id;
    }

    @PostMapping(value = "/{id}/autosave", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String autosave(
        @PathVariable Long id,
        @RequestParam(defaultValue = "{}") String body,
        @RequestParam(defaultValue = "[]") String footnotes,
        @RequestParam(defaultValue = "[]") String bibliography,
        @RequestParam(defaultValue = "0") int wordCount
    ) {
        LocalDateTime savedAt = paperService.saveBody(id, body, footnotes, bibliography, wordCount);
        return "{\"savedAt\":\"" + savedAt.format(TIME_FMT) + "\"}";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        paperService.delete(id);
        ra.addFlashAttribute("successMessage", "Paper deleted.");
        return "redirect:/papers";
    }
}
