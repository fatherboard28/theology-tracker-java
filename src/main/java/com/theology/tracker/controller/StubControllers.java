package com.theology.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

/**
 * Stub controllers for Phase 1.
 *
 * Each route returns the corresponding stub template so all navigation links
 * resolve without 404 errors. These controllers are replaced section-by-section
 * in Phases 3–15 with full implementations.
 */
@Controller
public class StubControllers {

    @GetMapping("/notes")
    public String notes() { return "notes/index"; }

    @GetMapping("/sessions")
    public String sessions() { return "sessions/index"; }

    @GetMapping("/methods")
    public String methods() { return "methods/index"; }

    @GetMapping("/calendar")
    public String calendar() { return "calendar/index"; }

    @GetMapping("/work-items")
    public String workItems() { return "work-items/index"; }

    @GetMapping("/settings")
    public String settings() { return "settings/index"; }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);
        return "search/index";
    }
}
