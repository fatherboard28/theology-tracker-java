package com.theology.tracker.controller;

import com.theology.tracker.dto.StudySessionFormDto;
import com.theology.tracker.model.StudySession;
import com.theology.tracker.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/sessions")
public class SessionController {

    private final StudySessionService sessionService;
    private final WorkItemService workItemService;
    private final MethodService methodService;
    private final TopicService topicService;

    public SessionController(
        StudySessionService sessionService,
        WorkItemService workItemService,
        MethodService methodService,
        TopicService topicService
    ) {
        this.sessionService = sessionService;
        this.workItemService = workItemService;
        this.methodService = methodService;
        this.topicService = topicService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", sessionService.findAll());
        return "sessions/index";
    }

    @GetMapping("/new")
    public String newForm(
        @RequestParam(required = false) Long workItemId,
        @RequestParam(required = false) Long methodId,
        @RequestParam(required = false) Long topicId,
        Model model
    ) {
        model.addAttribute("preWorkItemId", workItemId);
        model.addAttribute("preMethodId", methodId);
        model.addAttribute("preTopicId", topicId);
        model.addAttribute("allWorkItems", workItemService.findAll());
        model.addAttribute("allMethods", methodService.findAll());
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("formAction", "/sessions");
        model.addAttribute("pageTitle", "Log Study Session");
        return "sessions/form";
    }

    @PostMapping
    public String create(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
        @RequestParam Integer durationMinutes,
        @RequestParam(required = false) Long workItemId,
        @RequestParam(required = false) Long methodId,
        @RequestParam(required = false) String reflectionNote,
        @RequestParam(required = false) List<Long> topicIds,
        @RequestParam(required = false) List<String> scriptureTags,
        @RequestParam(required = false) String returnUrl,
        RedirectAttributes ra
    ) {
        StudySession session = sessionService.create(new StudySessionFormDto(
            sessionDate, durationMinutes, workItemId, methodId, reflectionNote, topicIds, scriptureTags
        ));
        ra.addFlashAttribute("successMessage", "Session logged.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/sessions/" + session.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        StudySession session = sessionService.findById(id);
        model.addAttribute("session", session);
        model.addAttribute("scriptureTags", sessionService.findScriptureTags(id));
        return "sessions/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        StudySession session = sessionService.findById(id);
        model.addAttribute("session", session);
        model.addAttribute("allWorkItems", workItemService.findAll());
        model.addAttribute("allMethods", methodService.findAll());
        model.addAttribute("allTopics", topicService.findAllOrdered());
        model.addAttribute("scriptureTags", sessionService.findScriptureTags(id));
        model.addAttribute("formAction", "/sessions/" + id);
        model.addAttribute("pageTitle", "Edit Session");
        return "sessions/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
        @RequestParam Integer durationMinutes,
        @RequestParam(required = false) Long workItemId,
        @RequestParam(required = false) Long methodId,
        @RequestParam(required = false) String reflectionNote,
        @RequestParam(required = false) List<Long> topicIds,
        @RequestParam(required = false) List<String> scriptureTags,
        RedirectAttributes ra
    ) {
        sessionService.update(id, new StudySessionFormDto(
            sessionDate, durationMinutes, workItemId, methodId, reflectionNote, topicIds, scriptureTags
        ));
        ra.addFlashAttribute("successMessage", "Session updated.");
        return "redirect:/sessions/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @RequestParam(required = false) String returnUrl,
        RedirectAttributes ra
    ) {
        sessionService.delete(id);
        ra.addFlashAttribute("successMessage", "Session deleted.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/sessions";
    }
}
