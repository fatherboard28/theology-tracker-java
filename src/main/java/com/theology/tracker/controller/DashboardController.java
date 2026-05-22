package com.theology.tracker.controller;

import com.theology.tracker.service.DashboardService;
import com.theology.tracker.service.ProgressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class DashboardController {

    private final ProgressService progressService;
    private final DashboardService dashboardService;

    public DashboardController(ProgressService progressService, DashboardService dashboardService) {
        this.progressService = progressService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentStreak", progressService.calculateCurrentStreak());
        model.addAttribute("longestStreak", progressService.calculateLongestStreak());

        var lastSession = progressService.getLastSession();
        model.addAttribute("lastSessionDate", lastSession.map(s -> s.getSessionDate()).orElse(null));
        model.addAttribute("lastSessionMinutes", lastSession.map(s -> s.getDurationMinutes()).orElse(null));

        model.addAttribute("weekMinutes", dashboardService.getWeekMinutes());
        model.addAttribute("monthMinutes", dashboardService.getMonthMinutes());
        model.addAttribute("activeCourses", dashboardService.getActiveCourses());
        model.addAttribute("activeTopics", dashboardService.getActiveTopics());
        model.addAttribute("upcomingDue", dashboardService.getUpcomingDue());
        model.addAttribute("recentWorkItems", dashboardService.getRecentWorkItems());
        model.addAttribute("recentNotes", dashboardService.getRecentNotes());
        model.addAttribute("heatmapDays", dashboardService.getHeatmapDays());

        return "dashboard/index";
    }
}
