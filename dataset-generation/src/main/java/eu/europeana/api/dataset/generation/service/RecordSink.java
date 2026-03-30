package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.oaipmh.model.Record;

public interface RecordSink {
    /**
     * Consume a single record.
     * @param record the OAI-PMH record
     * @throws Exception if storing/writing fails
     */
    void consume(Record record) throws Exception;

    /**
     * Close any resources (ZIP file, database connection, etc.).
     * @throws Exception
     */
    void close() throws Exception;
}
