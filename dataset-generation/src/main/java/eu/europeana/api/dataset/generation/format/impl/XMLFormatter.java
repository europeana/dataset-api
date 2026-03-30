package eu.europeana.api.dataset.generation.format.impl;

import eu.europeana.api.dataset.generation.format.DataFormatter;

import java.io.InputStream;

/**
 * A formatter that converts metadata streams into RDF XML format.
 * This class implements the DataFormatter interface to process metadata
 * streams and generate outputs in the XML (.xml) format.
 *
 * The formatter does not modify the contents as the input is already in XML format.
 */
public class XMLFormatter implements DataFormatter {

    @Override
    public InputStream format(InputStream metadataStream) {
        return metadataStream;
    }

    @Override
    public String getFileExtension() {
        return ".xml";
    }
}
