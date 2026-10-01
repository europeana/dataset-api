package eu.europeana.api.web;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enum representing valid dataset formats allowed for download.
 * The {@code label} is also the folder name under which archives of that format are stored.
 * Add a new enum constant (label + media type) when supporting an additional format.
 */
public enum FileTypes {
    XML("XML", "application/rdf+xml", "xml"),
    TTL("TTL", "text/turtle", "ttl");
    // Example for a future format:
    // JSONLD("JSONLD", "application/ld+json", "jsonld");

    private static final Map<String, FileTypes> BY_LABEL = new HashMap<>();

    static {
        for (FileTypes type : values()) {
            BY_LABEL.putIfAbsent(type.label.toUpperCase(Locale.ENGLISH), type);
        }
    }

    /**
     * Directory name associated with this format.
     */
    public final String label;

    /**
     * MIME / media type for this format (e.g. application/rdf+xml).
     */
    public final String mediaType;

    /**
     * File extension for content inside the zip (e.g. xml, ttl).
     */
    public final String extension;

    FileTypes(String label, String mediaType, String extension) {
        this.label = label;
        this.mediaType = mediaType;
        this.extension = extension;
    }

    /**
     * Validate if provided format matches a supported format.
     * @param fileExtension format to validate, can be null
     * @return {@code true} if supported, otherwise {@code false}
     */
    public static boolean isValid(String fileExtension) {
        return fromName(fileExtension) != null;
    }

    /**
     * Resolve format by directory label (e.g. {@code XML} → {@link #XML}).
     * @param label directory name
     * @return matching {@link FileTypes}, or {@code null} if unknown
     */
    public static FileTypes fromLabel(String label) {
        if (label == null) {
            return null;
        }
        return BY_LABEL.get(label.toUpperCase(Locale.ENGLISH));
    }

    /**
     * Resolve format by enum name (e.g. request param {@code format=xml}).
     * @param name format name
     * @return matching {@link FileTypes}, or {@code null} if unknown
     */
    public static FileTypes fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return FileTypes.valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
