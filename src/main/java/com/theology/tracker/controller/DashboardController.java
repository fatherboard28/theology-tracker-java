package com.theology.tracker.controller;

import com.theology.tracker.model.StudySession;
import com.theology.tracker.service.ProgressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

@Controller
public class DashboardController {

    private final ProgressService progressService;

    public DashboardController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentStreak", progressService.calculateCurrentStreak());
        model.addAttribute("longestStreak", progressService.calculateLongestStreak());

        Optional<StudySession> lastSession = progressService.getLastSession();
        model.addAttribute("lastSessionDate", lastSession.map(StudySession::getSessionDate).orElse(null));
        model.addAttribute("lastSessionMinutes", lastSession.map(StudySession::getDurationMinutes).orElse(null));

        // Stubs for remaining dashboard widgets — replaced in Phase 11
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
