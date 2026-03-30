package eu.europeana.api.dataset.generation.format;

import java.io.InputStream;

/**
 * An interface for formatting metadata streams into specific formats and retrieving
 * the file extension associated with the formatter.
 * <p>
 * Implementations should define how to process and transform metadata streams into
 * the desired output format.
 *
 * @author Srishti Singh
 * @since 23 march 2026
 */
public interface DataFormatter {

    InputStream format(InputStream metadataStream);

    String getFileExtension();
}
