package eu.europeana.api.dataset.generation.format;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.w3c.dom.Document;
import java.io.OutputStream;

/**
 * Defines the contract for formatting metadata into specific output formats.
 * Implementations of this interface specify how to transform metadata stored
 * in a {@link Document} into a desired format and write it to an {@link OutputStream}.
 * Additionally, the file extension corresponding to the specific output format
 * can be retrieved using this interface.
 */
public interface DataFormatter {

    /**
     * Writes the metadata from the specified {@link Document} to the provided {@link OutputStream}.
     * The method transforms the metadata into a specific output format and streams the formatted
     * content to the given output stream.
     *
     * @param metadata the {@link Document} containing metadata to be formatted and written
     * @param zipOut the {@link OutputStream} to which the formatted metadata is written
     * @throws EuropeanaApiException if an error occurs during the metadata transformation or writing process
     */
    void write(Document metadata, OutputStream zipOut) throws EuropeanaApiException;

    /**
     * Retrieves the file extension associated with the output format handled by this formatter.
     * The file extension typically denotes the format of the output, such as ".xml" or ".json".
     *
     * @return the file extension as a string, including the leading dot (e.g., ".xml").
     */
    String getFileExtension();
}
