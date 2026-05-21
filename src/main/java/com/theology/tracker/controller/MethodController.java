package com.theology.tracker.controller;

import com.theology.tracker.dto.MethodFormDto;
import com.theology.tracker.model.Method;
import com.theology.tracker.model.NoteParentType;
import com.theology.tracker.service.MethodService;
import com.theology.tracker.service.NoteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/methods")
public class MethodController {

    private final MethodService methodService;
    private final NoteService noteService;

    public MethodController(MethodService methodService, NoteService noteService) {
        this.methodService = methodService;
        this.noteService = noteService;
    }

    @GetMapping
    public String list(Model model) {
        List<Method> methods = methodService.findAll();
        model.addAttribute("methods", methods);
        model.addAttribute("usageCounts", methods.stream()
            .collect(java.util.stream.Collectors.toMap(
                Method::getId,
                m -> methodService.countUsages(m.getId())
            )));
        return "methods/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("method", new Method());
        model.addAttribute("formAction", "/methods");
        model.addAttribute("pageTitle", "New Method");
        return "methods/form";
    }

    @PostMapping
    public String create(
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String personalNotes,
        RedirectAttributes ra
    ) {
        Method method = methodService.create(new MethodFormDto(name, description, personalNotes));
        ra.addFlashAttribute("successMessage", "Method \"" + method.getName() + "\" created.");
        return "redirect:/methods/" + method.getId();
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Method method = methodService.findById(id);
        model.addAttribute("method", method);
        model.addAttribute("sessions", methodService.findSessions(id));
        model.addAttribute("usageCount", methodService.countUsages(id));
        model.addAttribute("notes", noteService.findByParent(NoteParentType.METHOD, id));
        return "methods/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("method", methodService.findById(id));
        model.addAttribute("formAction", "/methods/" + id);
        model.addAttribute("pageTitle", "Edit Method");
        return "methods/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) String personalNotes,
        RedirectAttributes ra
    ) {
        methodService.update(id, new MethodFormDto(name, description, personalNotes));
        ra.addFlashAttribute("successMessage", "Method updated.");
        return "redirect:/methods/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Method method = methodService.findById(id);
        methodService.delete(id);
        ra.addFlashAttribute("successMessage", "Method \"" + method.getName() + "\" deleted.");
        return "redirect:/methods";
    }
}
