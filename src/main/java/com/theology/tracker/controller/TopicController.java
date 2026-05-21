package com.theology.tracker.controller;

import com.theology.tracker.dto.TopicFormDto;
import com.theology.tracker.model.Topic;
import com.theology.tracker.model.TopicType;
import com.theology.tracker.service.TopicService;
import com.theology.tracker.service.WorkItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;
    private final WorkItemService workItemService;

    public TopicController(TopicService topicService, WorkItemService workItemService) {
        this.topicService = topicService;
        this.workItemService = workItemService;
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
        model.addAttribute("topic", topic);
        model.addAttribute("rootTopics", rootTopics);
        model.addAttribute("ownedWorkItems", workItemService.findByOwningTopic(id));
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
}
