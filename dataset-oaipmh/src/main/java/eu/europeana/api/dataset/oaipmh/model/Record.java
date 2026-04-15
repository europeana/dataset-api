package eu.europeana.api.dataset.oaipmh.model;

import org.w3c.dom.Document;

/**
 * Represents a single OAI-PMH record with an identifier and associated metadata.
 * Instances of this class are immutable and are typically used to encapsulate metadata
 * retrieved from an OAI-PMH source.
 *
 * The record consists of:
 * - An identifier, which uniquely identifies the record.
 * - A metadata , which contains the associated metadata content of the record.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
public class Record {

    public final String identifier;
    public final Document metadata;

    public Record(String identifier, Document metadata) {
        this.identifier = identifier;
        this.metadata = metadata;
    }

    public String getIdentifier() {
        return identifier;
    }
}
