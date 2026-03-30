package eu.europeana.api.dataset.generation.service.impl;

import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.oaipmh.model.Record;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
// TODO may be rename it to CompositeRecordSink OR MultiFormatRecordSink
public class MultiRecordSink implements RecordSink, Closeable {

    private final List<RecordSink> sinks;

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
    public void consume(Record record) throws Exception {
        byte[] data = record.metadataStream.readAllBytes(); // buffer once
        for (RecordSink sink : sinks) {
            sink.consume(new Record(record.getIdentifier(),
                    new ByteArrayInputStream(data)));
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