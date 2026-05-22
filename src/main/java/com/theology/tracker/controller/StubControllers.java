package com.theology.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Stub controllers for Phase 1.
 *
 * Each route returns the corresponding stub template so all navigation links
 * resolve without 404 errors. These controllers are replaced section-by-section
 * in Phases 3–15 with full implementations.
 */
@Controller
public class StubControllers {

    @GetMapping("/settings")
    public String settings() { return "settings/index"; }
}
