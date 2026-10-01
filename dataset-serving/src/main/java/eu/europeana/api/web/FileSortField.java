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
     * Ascending comparator for this field.
     */
    public Comparator<FileDetails> comparator() {
        return switch (this) {
            case NAME -> Comparator.comparing(
                FileDetails::getFileName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case SIZE -> Comparator.comparing(
                FileSortField::parseSize, Comparator.nullsLast(Long::compareTo));
            case LAST_MODIFIED -> Comparator.comparing(
                FileSortField::parseLastModified, Comparator.nullsLast(Instant::compareTo));
        };
    }

    private static Long parseSize(FileDetails details) {
        if (details.getFileSize() == null) {
            return null;
        }
        try {
            return Long.parseLong(details.getFileSize().trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
