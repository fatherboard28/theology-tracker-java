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

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) return minutes + "m";
        if (minutes == 0) return hours + "h";
        return hours + "h " + minutes + "m";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentStreak", progressService.calculateCurrentStreak());
        model.addAttribute("longestStreak", progressService.calculateLongestStreak());

        var lastSession = progressService.getLastSession();
        model.addAttribute("lastSessionDate", lastSession.map(s -> s.getSessionDate()).orElse(null));
        model.addAttribute("lastSessionMinutes", lastSession.map(s -> s.getDurationMinutes()).orElse(null));

        model.addAttribute("weekMinutes", formatMinutes(dashboardService.getWeekMinutes()));
        model.addAttribute("monthMinutes", formatMinutes(dashboardService.getMonthMinutes()));
        model.addAttribute("activeCourses", dashboardService.getActiveCourses());
        model.addAttribute("activeTopics", dashboardService.getActiveTopics());
        model.addAttribute("upcomingDue", dashboardService.getUpcomingDue());
        model.addAttribute("recentWorkItems", dashboardService.getRecentWorkItems());
        model.addAttribute("recentNotes", dashboardService.getRecentNotes());
        model.addAttribute("heatmapDays", dashboardService.getHeatmapDays());

        return "dashboard/index";
    }
}
