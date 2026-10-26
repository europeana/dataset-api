package eu.europeana.api.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;

/**
 * Sort fields supported by the dataset file listing endpoint.
 */
public enum FileSortField {
    NAME,
    SIZE,
    LAST_MODIFIED;

    /**
     * Parse a request parameter into a sort field.
     * @param value request value (name, size, lastModified); {@code null} defaults to {@link #NAME}
     * @return matching sort field
     * @throws IllegalArgumentException if the value is not supported
     */
    public static FileSortField fromParam(String value) {
        if (value == null || value.isBlank()) {
            return NAME;
        }
        return switch (value.trim().toLowerCase(Locale.ENGLISH)) {
            case "name" -> NAME;
            case "size" -> SIZE;
            case "lastmodified", "last_modified" -> LAST_MODIFIED;
            default -> throw new IllegalArgumentException(
                "Unsupported sortBy: " + value + ". Allowed: name, size, lastModified");
        };
    }

    /**
     * Returns   comparator for fields with null value at last.
     */
    public Comparator<FileDetails> comparator() {
        return switch (this) {
            //To sort alphabetically from A to Z (case-insensitive).
            case NAME -> Comparator.comparing(
                FileDetails::getFileName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            //To sort from smallest to largest file size.
            case SIZE -> Comparator.comparing(
                FileDetails::getFileSize, Comparator.nullsLast(Long::compareTo));
            //To sort from the earliest date to latest date.
            case LAST_MODIFIED -> Comparator.comparing(
                FileSortField::parseLastModified, Comparator.nullsLast(Instant::compareTo));
        };
    }
    private static Instant parseLastModified(FileDetails details) {
        if (details.getLastModified() == null) {
            return null;
        }
        try {
            return Instant.parse(details.getLastModified());
        } catch (Exception e) {
            return null;
        }
    }
}
