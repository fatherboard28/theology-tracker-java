package com.theology.tracker.controller;

import com.theology.tracker.dto.TopicFormDto;
import com.theology.tracker.model.Topic;
import com.theology.tracker.service.NoteService;
import com.theology.tracker.service.PaperService;
import com.theology.tracker.service.TopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;
    private final NoteService noteService;
    private final PaperService paperService;

    public TopicController(TopicService topicService, NoteService noteService, PaperService paperService) {
        this.topicService = topicService;
        this.noteService = noteService;
        this.paperService = paperService;
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
            topic.setParentTopic(topicService.findById(parentTopicId));
        }
        model.addAttribute("topic", topic);
        model.addAttribute("allTopics", topicService.findAll());
        model.addAttribute("formAction", "/topics");
        model.addAttribute("pageTitle", "New Topic");
        return "topics/form";
    }

    @PostMapping
    public String create(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Long parentTopicId,
        RedirectAttributes ra
    ) {
        Topic topic = topicService.create(new TopicFormDto(title, description, parentTopicId));
        ra.addFlashAttribute("successMessage", "Topic \"" + topic.getTitle() + "\" created.");
        return "redirect:/topics/" + topic.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Topic topic = topicService.findById(id);
        model.addAttribute("topic", topic);
        model.addAttribute("notes", noteService.findByTopic(id));
        model.addAttribute("papers", paperService.findByTopic(id));
        model.addAttribute("allTopics", topicService.findAll());
        return "topics/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Topic topic = topicService.findById(id);
        model.addAttribute("topic", topic);
        model.addAttribute("allTopics", topicService.findAll().stream()
            .filter(t -> !t.getId().equals(id))
            .toList());
        model.addAttribute("formAction", "/topics/" + id);
        model.addAttribute("pageTitle", "Edit Topic");
        return "topics/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Long parentTopicId,
        RedirectAttributes ra
    ) {
        topicService.update(id, new TopicFormDto(title, description, parentTopicId));
        ra.addFlashAttribute("successMessage", "Topic updated.");
        return "redirect:/topics/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        topicService.delete(id);
        ra.addFlashAttribute("successMessage", "Topic deleted.");
        return "redirect:/topics";
    }
}
