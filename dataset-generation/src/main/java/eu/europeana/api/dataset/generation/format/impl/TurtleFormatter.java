package eu.europeana.api.dataset.generation.format.impl;

import eu.europeana.api.commons_sb3.definitions.utils.TurtleRecordWriter;

import java.io.*;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.exception.DataFormatterException;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput1.DOM2Model;
import org.apache.jena.shared.JenaException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import javax.xml.transform.TransformerFactory;

/**
 * A formatter that converts metadata in the form of a DOM {@link Document} into Turtle (.ttl) format.
 *
 * Responsibilities:
 * - Transforms a DOM {@link Document} into a Jena {@link Model}.
 * - Writes the Jena {@link Model} to a {@link ByteArrayOutputStream} in Turtle format.
 * - Streams the resulting Turtle data to the provided {@link OutputStream}.
 * - Provides the standard file extension for Turtle files (".ttl").
 *
 * Errors:
 * - Errors during the transformation of the {@link Document} into a {@link Model} are wrapped
 *   in a {@link DataFormatterException}.
 * - I/O errors encountered while writing the Turtle data to the {@link OutputStream} are
 *   also wrapped in a {@link DataFormatterException} for consistent error handling.
 *
 * Implements {@link DataFormatter}.
 */
public record TurtleFormatter(TransformerFactory transformerFactory) implements DataFormatter {

    private static final Logger LOG = LogManager.getLogger(TurtleFormatter.class);

    // TODO find a solution to not close zip
    @Override
    public void write(String recordId, Document metadata, OutputStream zipOut) throws EuropeanaApiException {
        OutputStream safeOut = new FilterOutputStream(zipOut) {
            @Override
            public void close() throws IOException {
                flush(); // prevent closing zipOut
            }
        };

        try (TurtleRecordWriter writer = new TurtleRecordWriter(safeOut)) {
            Model m = toModel(metadata);
           if (m != null) {
               writer.write(m);
           } else {
               LOG.error("Skipping record - {} , due to invalid data found - " , recordId);
           }
        } catch (IOException e) {
            throw new DataFormatterException("Error writing the metadata in turtle format " + e.getMessage(), e);
        } catch (NoSuchFieldException | IllegalAccessException e) {
           LOG.error("Error disabling the errorForSpaceInURI field in ReaderRDFXML_ARP1 for the TurtleRecordWriter " + e.getMessage(), e);
        }
    }

    /**
     * Converts a {@link Document} into a Jena {@link Model}.
     * The method processes the provided DOM {@link Document} using the DOM2Model utility to create and populate a Jena {@link Model}.
     *
     * @param doc The {@link Document} to be converted into a Jena {@link Model}.
     * @return A {@link Model} representation of the input {@link Document}.
     * @throws DataFormatterException If an error occurs during the transformation of the {@link Document} into a {@link Model}.
     */
    public static Model toModel(Document doc) throws DataFormatterException {
        try {
            Model m = ModelFactory.createDefaultModel();
            DOM2Model dom2Model = DOM2Model.createD2M("", m);
            dom2Model.load(doc);
            return m;
        } catch (Exception e) {
             // for all invalid jena errors, skip those records and Log them
            // TODO add stacktrace later
            if (e instanceof JenaException) {
                LOG.error("Invalid data found  - " + e.getMessage());
                return null;
            }
            throw new DataFormatterException("Error converting document to Jena model - " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileExtension() {
        return ".ttl";
    }
}
