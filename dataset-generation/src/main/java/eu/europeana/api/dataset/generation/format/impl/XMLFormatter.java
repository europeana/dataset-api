package eu.europeana.api.dataset.generation.format.impl;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.exception.DataFormatterException;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

/**
 * A formatter that converts metadata in the form of a DOM {@link Document} into XML format.
 * This class implements the {@link DataFormatter} interface, providing methods to write
 * metadata data to an {@link OutputStream} in a well-formed XML format and retrieve the
 * corresponding file extension.
 *
 * Errors:
 * - Errors during the transformation process or output handling are wrapped in a
 *   {@link DataFormatterException} for consistent error management.
 *
 */
public record XMLFormatter(TransformerFactory transformerFactory) implements DataFormatter {

    @Override
    public void write(Document metadata, OutputStream zipOut) throws EuropeanaApiException {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(metadata), new StreamResult(zipOut));
        } catch (TransformerException e) {
            throw new DataFormatterException("Error writing the metadata in xml format - " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileExtension() {
        return ".xml";
    }
}
