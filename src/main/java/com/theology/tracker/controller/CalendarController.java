package com.theology.tracker.controller;

import com.theology.tracker.service.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public String calendar(
        @RequestParam(defaultValue = "month") String view,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        Model model
    ) {
        LocalDate today = LocalDate.now();
        model.addAttribute("view", view);
        model.addAttribute("today", today);

        if ("week".equals(view)) {
            LocalDate weekDate = date != null ? date : today;
            LocalDate monday = weekDate.with(DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            model.addAttribute("weekDays", calendarService.getWeekDays(weekDate));
            model.addAttribute("weekStart", monday);
            model.addAttribute("weekEnd", sunday);
            model.addAttribute("prevWeek", monday.minusWeeks(1));
            model.addAttribute("nextWeek", monday.plusWeeks(1));
        } else if ("agenda".equals(view)) {
            model.addAttribute("agendaItems", calendarService.getAgendaItems());
        } else {
            YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();
            model.addAttribute("yearMonth", ym);
            model.addAttribute("prevMonth", ym.minusMonths(1));
            model.addAttribute("nextMonth", ym.plusMonths(1));
            model.addAttribute("monthDays", calendarService.getMonthDays(ym));
            model.addAttribute("monthLabel", ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            model.addAttribute("view", "month");
        }

        return "calendar/index";
    }
}
