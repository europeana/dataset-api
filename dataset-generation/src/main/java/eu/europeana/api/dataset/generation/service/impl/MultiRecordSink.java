package eu.europeana.api.dataset.generation.service.impl;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.exception.DatasetGenerationException;
import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.oaipmh.model.Record;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * A {@code MultiRecordSink} is a composite implementation of the {@link RecordSink} interface
 * that delegates the consumption of records to multiple underlying {@link RecordSink} instances.
 * It also implements {@link Closeable} to ensure proper resource management for all sinks.
 *
 * This class is useful for scenarios where multiple sinks need to process the same record
 * concurrently, such as writing records to a combination of storage systems or formats.
 */
public class MultiRecordSink implements RecordSink, Closeable {

    private final List<RecordSink> sinks;

    /**
     * Constructs a {@code MultiRecordSink} with the specified list of sinks.
     * @param sinks sinks to be added to the composite
     */
    public MultiRecordSink(List<RecordSink> sinks) {
        this.sinks = sinks;
    }

    /**
     * Consumes a {@link java.lang.Record} by buffering its metadata stream and distributing it
     * to all connected {@link RecordSink} instances.
     *
     * The metadata stream of the provided record is read completely into memory
     * and reused across all sinks, avoiding multiple reads of the same stream.
     * Each sink receives a new {@link java.lang.Record} instance containing the buffered data.
     *
     * @param record the record to be consumed; must contain an identifier and a metadata stream
     * @throws Exception if an error occurs while reading the metadata stream or distributing the record to sinks
     */
    @Override
    public void consume(Record record) throws EuropeanaApiException {
        for (RecordSink sink : sinks) {
            sink.consume(record);
        }
    }

    @Override
    public void close() throws IOException {
        for (RecordSink sink : sinks) {
            if (sink instanceof Closeable) {
                ((Closeable) sink).close();
            }
        }
    }
}
