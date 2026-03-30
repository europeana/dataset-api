package eu.europeana.api.dataset.generation.format.impl;

import eu.europeana.api.commons_sb3.definitions.utils.TurtleRecordWriter;

import java.io.*;

import eu.europeana.api.dataset.generation.format.DataFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A formatter that converts metadata streams into Turtle RDF format.
 * This class implements the DataFormatter interface to process metadata
 * streams and generate outputs in the Turtle (.ttl) format.
 *
 * The formatter reads RDF/XML data from the provided InputStream, converts it
 * into a Jena Model, and writes the model in Turtle format to an OutputStream.
 * The resulting output is then converted back into an InputStream for further use.
 *
 * Any errors that occur during the formatting process are logged.
 */
public class TurtleFormatter implements DataFormatter {

    private static final Logger LOG = LogManager.getLogger(TurtleFormatter.class);

    @Override
    public InputStream format(InputStream metadataStream) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             TurtleRecordWriter writer = new TurtleRecordWriter(outputStream)) {

            Model modelResult = ModelFactory
                    .createDefaultModel()
                    .read(metadataStream, "", "RDF/XML");

            writer.write(modelResult);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (IOException e) {
            LOG.error("Error generating turtle output", e);
        }
        return null;
    }

    @Override
    public String getFileExtension() {
        return ".ttl";
    }
}
