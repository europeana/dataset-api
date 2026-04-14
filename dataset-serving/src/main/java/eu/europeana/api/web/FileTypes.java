package eu.europeana.api.web;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum Representing valid file extensions allowed to download data-sets along with their file type.
 * The 'label' for file type also represents the 'folder name' inside which that particular type of file archives are available.
 */
public enum FileTypes {
    XML("XML"), TTL("TURTLE");
    public final String label;

    private static final Map<String, String> fileExtensions = new HashMap<>();
    static {
        for (FileTypes e : values()) {
            fileExtensions.putIfAbsent(e.label,e.name());
        }
    }

    FileTypes(String label) {
        this.label = label;
    }

    /**
     * Validate if provided fileExtension matches valid file extension.
     * @param fileExtension file extension to validate ,can be null.
     * @return  {@code true} if exists and part of supported extensions map otherwise {@code false}.
     */
    public static boolean isValid(String fileExtension) {
        return fileExtension != null &&  fileExtensions.containsValue(fileExtension.toUpperCase());
    }

    public static String getTypeByLabel(String label){
      return fileExtensions.get(label);
    }
}
