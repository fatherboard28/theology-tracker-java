package com.theology.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theology.tracker.dto.export.FullExportDto;
import com.theology.tracker.service.DataExportService;
import com.theology.tracker.service.DataImportService;
import com.theology.tracker.service.MarkdownExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class ExportController {

    private final DataExportService exportService;
    private final DataImportService importService;
    private final MarkdownExportService markdownService;
    private final ObjectMapper objectMapper;

    public ExportController(
        DataExportService exportService,
        DataImportService importService,
        MarkdownExportService markdownService,
        ObjectMapper objectMapper
    ) {
        this.exportService = exportService;
        this.importService = importService;
        this.markdownService = markdownService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings/index";
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson() throws IOException {
        FullExportDto data = exportService.buildExport();
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        String filename = "theology-tracker-export-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(bytes);
    }

    @GetMapping("/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown() {
        String markdown = markdownService.buildMarkdownExport();
        byte[] bytes = markdown.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "theology-tracker-notes-" + LocalDate.now() + ".md";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(bytes);
    }

    @PostMapping("/import")
    public String importJson(
        @RequestParam("file") MultipartFile file,
        RedirectAttributes ra
    ) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "No file selected for import.");
            return "redirect:/settings";
        }
        try {
            FullExportDto data = objectMapper.readValue(file.getInputStream(), FullExportDto.class);
            importService.importData(data);
            ra.addFlashAttribute("successMessage", "Import completed successfully. All data has been restored.");
        } catch (IOException e) {
            ra.addFlashAttribute("errorMessage", "Failed to parse import file: " + e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Import failed: " + e.getMessage());
        }
        return "redirect:/settings";
    }
}
