package com.theology.tracker.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter WRITE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter[] READ_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    };

    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.format(WRITE);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (DateTimeFormatter fmt : READ_FORMATS) {
            try {
                return LocalDateTime.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Cannot parse LocalDateTime from SQLite value: " + raw);
    }
}
