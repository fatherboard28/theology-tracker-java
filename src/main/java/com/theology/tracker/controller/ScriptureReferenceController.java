package com.theology.tracker.controller;

import com.theology.tracker.dto.ScriptureReferenceResultDto;
import com.theology.tracker.service.ScriptureReferenceService;
import com.theology.tracker.service.ScriptureReferenceValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/scripture")
public class ScriptureReferenceController {

    private final ScriptureReferenceService referenceService;
    private final ScriptureReferenceValidator validator;

    public ScriptureReferenceController(
        ScriptureReferenceService referenceService,
        ScriptureReferenceValidator validator
    ) {
        this.referenceService = referenceService;
        this.validator = validator;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String ref, Model model) {
        if (ref != null && !ref.isBlank()) {
            ScriptureReferenceResultDto result = referenceService.findByReference(ref);
            model.addAttribute("result", result);
            model.addAttribute("query", ref);
        }
        model.addAttribute("bookAbbreviations", ScriptureReferenceValidator.BOOK_ABBREVIATIONS
            .stream().sorted().toList());
        return "scripture/index";
    }

    @PostMapping("/validate")
    public String validateRedirect(@RequestParam String ref) {
        return "redirect:/scripture?ref=" + ref.trim();
    }
}
