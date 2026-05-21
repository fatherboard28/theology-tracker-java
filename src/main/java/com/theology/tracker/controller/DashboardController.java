package com.theology.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

/**
 * Dashboard controller.
 *
 * Phase 1: returns stub empty model so the application starts and the base
 * layout can be verified. Full dashboard data queries are implemented in Phase 11.
 */
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // ── Stub data — replaced by real queries in Phase 11 ────────────────
        model.addAttribute("currentStreak",  0);
        model.addAttribute("longestStreak",  0);
        model.addAttribute("weekMinutes",    0);
        model.addAttribute("monthMinutes",   0);
        model.addAttribute("activeCourses",  Collections.emptyList());
        model.addAttribute("activeTopics",   Collections.emptyList());
        model.addAttribute("upcomingDue",    Collections.emptyList());
        model.addAttribute("recentNotes",    Collections.emptyList());
        model.addAttribute("recentWorkItems",Collections.emptyList());

        return "dashboard/index";
    }
}
