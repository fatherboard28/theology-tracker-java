package com.theology.tracker.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    private static final DateTimeFormatter WRITE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter[] READ_FORMATS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    };

    @Override
    public String convertToDatabaseColumn(LocalDate value) {
        return value == null ? null : value.format(WRITE);
    }

    @Override
    public LocalDate convertToEntityAttribute(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (DateTimeFormatter fmt : READ_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Cannot parse LocalDate from SQLite value: " + raw);
    }
}
