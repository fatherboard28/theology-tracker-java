package com.theology.tracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration.
 *
 * Registers:
 *  - Static resource handlers for CSS, JS, and fonts
 *  - ISO date/time formatters so LocalDate / LocalDateTime bind correctly
 *    from HTML date inputs (value="YYYY-MM-DD")
 *  - Root redirect to dashboard
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // No caching during development
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to the dashboard
        registry.addRedirectViewController("/", "/dashboard");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Register ISO-8601 date formatters so HTML date inputs bind to LocalDate
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }
}
