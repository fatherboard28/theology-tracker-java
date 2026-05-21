package com.theology.tracker.controller;

import com.theology.tracker.dto.UnitFormDto;
import com.theology.tracker.model.Unit;
import com.theology.tracker.service.UnitService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long courseId, Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("unit", new Unit());
        model.addAttribute("formAction", "/courses/" + courseId + "/units");
        model.addAttribute("pageTitle", "New Unit");
        return "units/form";
    }

    @PostMapping
    public String create(
        @PathVariable Long courseId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetCompletion,
        RedirectAttributes ra
    ) {
        UnitFormDto form = new UnitFormDto(title, description, targetCompletion);
        unitService.create(courseId, form);
        ra.addFlashAttribute("successMessage", "Unit added.");
        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/{unitId}/edit")
    public String editForm(@PathVariable Long courseId, @PathVariable Long unitId, Model model) {
        Unit unit = unitService.findById(unitId);
        model.addAttribute("courseId", courseId);
        model.addAttribute("unit", unit);
        model.addAttribute("formAction", "/courses/" + courseId + "/units/" + unitId);
        model.addAttribute("pageTitle", "Edit Unit");
        return "units/form";
    }

    @PostMapping("/{unitId}")
    public String update(
        @PathVariable Long courseId,
        @PathVariable Long unitId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetCompletion,
        RedirectAttributes ra
    ) {
        UnitFormDto form = new UnitFormDto(title, description, targetCompletion);
        unitService.update(unitId, form);
        ra.addFlashAttribute("successMessage", "Unit updated.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/{unitId}/delete")
    public String delete(@PathVariable Long courseId, @PathVariable Long unitId, RedirectAttributes ra) {
        unitService.delete(unitId);
        ra.addFlashAttribute("successMessage", "Unit deleted.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/reorder")
    @ResponseBody
    public ResponseEntity<Void> reorder(
        @PathVariable Long courseId,
        @RequestParam String ids
    ) {
        List<Long> orderedIds = Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .toList();
        unitService.reorder(courseId, orderedIds);
        return ResponseEntity.ok().build();
    }
}
