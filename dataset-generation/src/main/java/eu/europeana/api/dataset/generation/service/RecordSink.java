package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.exception.DatasetGenerationException;
import eu.europeana.api.dataset.oaipmh.model.Record;

import java.io.IOException;

/**
 * The {@code RecordSink} interface defines the contract for consuming and processing
 * OAI-PMH {@link java.lang.Record} objects. Implementations of this interface handle specific
 * use cases for storing, managing, or transforming records.
 *
 * Implementing classes must provide mechanisms to:
 * - Consume individual {@link java.lang.Record} instances via the {@link #consume(eu.europeana.api.dataset.oaipmh.model.Record)} method.
 * - Release or finalize resources when processing is complete through the {@link #close()} method.
 */
public interface RecordSink {
    /**
     * Consume a single record.
     * @param record the OAI-PMH record
     * @throws EuropeanaApiException if storing/writing fails
     */
    void consume(Record record) throws EuropeanaApiException;

    /**
     * Close any resources (ZIP file, database connection, etc.).
     * @throws IOException
     */
    void close() throws IOException;
}
