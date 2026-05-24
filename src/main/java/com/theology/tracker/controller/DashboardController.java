package com.theology.tracker.controller;

import com.theology.tracker.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("activeCourses", dashboardService.getActiveCoursesWithProgress());
        model.addAttribute("recentNotes", dashboardService.getRecentNotes());
        model.addAttribute("recentPapers", dashboardService.getRecentPapers());
        return "dashboard/index";
    }
}
