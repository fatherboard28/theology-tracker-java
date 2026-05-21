package com.theology.tracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.theology.tracker.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global error handler.
 * Renders friendly Thymeleaf error pages instead of the default Spring whitelabel.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("status",  404);
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path",    request.getRequestURI());
        return "error/error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("status",  404);
        model.addAttribute("message", "The page you're looking for doesn't exist.");
        model.addAttribute("path",    request.getRequestURI());
        return "error/error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("status",  404);
        model.addAttribute("message", "Resource not found.");
        model.addAttribute("path",    request.getRequestURI());
        return "error/error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("status",  400);
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        model.addAttribute("status",  500);
        model.addAttribute("message", "An unexpected error occurred.");
        model.addAttribute("detail",  ex.getMessage());
        return "error/error";
    }
}
