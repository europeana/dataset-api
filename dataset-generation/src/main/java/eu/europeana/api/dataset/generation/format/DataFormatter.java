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

    void write(Document metadata, OutputStream zipOut) throws EuropeanaApiException;

    String getFileExtension();
}
