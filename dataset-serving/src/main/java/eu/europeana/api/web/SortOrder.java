package eu.europeana.api.web;

import java.util.Locale;

/**
 * Sort direction for the dataset file listing endpoint.
 */
public enum SortOrder {
    ASC,
    DESC;

    /**
     * Parse a request parameter into a sort order.
     * @param value request value (asc, desc); {@code null} defaults to {@link #ASC}
     * @return matching sort order
     * @throws IllegalArgumentException if the value is not supported
     */
    public static SortOrder fromParam(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return switch (value.trim().toLowerCase(Locale.ENGLISH)) {
            case "asc", "ascending" -> ASC;
            case "desc", "descending" -> DESC;
            default -> throw new IllegalArgumentException(
                "Unsupported sortOrder: " + value + ". Allowed: asc, desc");
        };
    }
}
