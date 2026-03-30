package eu.europeana.api.dataset.oaipmh.model;

import java.io.InputStream;

/**
 * Represents a single OAI-PMH record with an identifier and associated metadata stream.
 * Instances of this class are immutable and are typically used to encapsulate metadata
 * retrieved from an OAI-PMH source.
 *
 * The record consists of:
 * - An identifier, which uniquely identifies the record.
 * - A metadata stream, which contains the associated metadata content of the record.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
public class Record {

    public final String identifier;
    public final InputStream metadataStream;

    public Record(String identifier, InputStream metadataStream) {
        this.identifier = identifier;
        this.metadataStream = metadataStream;
    }

    public String getIdentifier() {
        return identifier;
    }
}
