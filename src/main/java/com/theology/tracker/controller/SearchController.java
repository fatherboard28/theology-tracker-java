package com.theology.tracker.controller;

import com.theology.tracker.dto.SearchResultDto;
import com.theology.tracker.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);
        if (q != null && !q.isBlank()) {
            SearchResultDto result = searchService.search(q);
            model.addAttribute("result", result);
        }
        return "search/index";
    }
}
