package eu.europeana.api.web;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enum Representing valid file extensions allowed to download data-sets along with their file type.
 * The 'label' for file type also represents the 'folder name' inside which that particular type of file archives should be available.
 */
public enum FileTypes {
    XML("XML"), TTL("TTL");

    private static final Map<String, String> fileExtensions = new HashMap<>();
    static {
        for (FileTypes e : values()) {
            fileExtensions.putIfAbsent(e.label,e.name());
        }
    }

    /**
     * Represents name of directory associated to file extension
     */
    public final String label;

    FileTypes(String label) {
        this.label = label;
    }


    /**
     * Validate if provided fileExtension matches valid file extension.
     * @param fileExtension file extension to validate ,can be null.
     * @return  {@code true} if exists and part of supported extensions map otherwise {@code false}.
     */
    public static boolean isValid(String fileExtension) {
        return fileExtension != null &&  fileExtensions.containsValue(fileExtension.toUpperCase(
            Locale.ENGLISH));
    }

    /**
     * Return the file extension for the requested value.
     * e.g. TTL for the label 'TURTLE'
     * @param label directory name
     * @return value of file extension associated to requested label
     */
    public static String getTypeByLabel(String label){
      return fileExtensions.get(label);
    }


}
