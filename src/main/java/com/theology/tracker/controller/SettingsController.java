package com.theology.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static volatile String defaultAuthor = "";

    public static String getDefaultAuthor() {
        return defaultAuthor;
    }

    @GetMapping
    public String show(Model model) {
        model.addAttribute("defaultAuthor", defaultAuthor);
        return "settings/index";
    }

    @PostMapping
    public String save(@RequestParam(required = false, defaultValue = "") String defaultAuthor,
                       RedirectAttributes ra) {
        SettingsController.defaultAuthor = defaultAuthor.trim();
        ra.addFlashAttribute("successMessage", "Settings saved.");
        return "redirect:/settings";
    }
}
