package com.theology.tracker.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScriptureReferenceValidator {

    public static final Set<String> BOOK_ABBREVIATIONS = Set.of(
        // Old Testament
        "Gen", "Exo", "Lev", "Num", "Deu", "Jos", "Jdg", "Rut",
        "1Sa", "2Sa", "1Ki", "2Ki", "1Ch", "2Ch", "Ezr", "Neh",
        "Est", "Job", "Psa", "Pro", "Ecc", "Son", "Isa", "Jer",
        "Lam", "Eze", "Dan", "Hos", "Joe", "Amo", "Oba", "Jon",
        "Mic", "Nah", "Hab", "Zep", "Hag", "Zec", "Mal",
        // New Testament
        "Mat", "Mrk", "Luk", "Jhn", "Act", "Rom", "1Co", "2Co",
        "Gal", "Eph", "Phi", "Col", "1Th", "2Th", "1Ti", "2Ti",
        "Tit", "Phm", "Heb", "Jam", "1Pe", "2Pe", "1Jo", "2Jo",
        "3Jo", "Jud", "Rev"
    );

    // Matches: BOOK CHAPTER  or  BOOK CHAPTER:VERSE  or  BOOK CHAPTER:VERSE–VERSE
    private static final Pattern REFERENCE_PATTERN =
        Pattern.compile("^([1-9]?[A-Z][a-z]{2}) (\\d+)(?::(\\d+)(?:[–\\-](\\d+))?)?$");

    public boolean isValid(String reference) {
        if (reference == null || reference.isBlank()) return false;
        Matcher m = REFERENCE_PATTERN.matcher(reference.trim());
        return m.matches() && BOOK_ABBREVIATIONS.contains(m.group(1));
    }

    public void validate(String reference) {
        if (!isValid(reference)) {
            throw new IllegalArgumentException(
                "Invalid scripture reference: \"" + reference + "\". " +
                "Expected format: AAA C:V (e.g. Rom 8:28, Gen 1:1, Psa 119).");
        }
    }

    /**
     * Extracts the book abbreviation and chapter prefix from a query string,
     * used to build a prefix search for chapter-level matching.
     * e.g. "Rom 8" → "Rom 8:" prefix for findByReferenceStartingWith
     */
    public boolean isChapterQuery(String query) {
        return query != null && !query.contains(":");
    }
}
