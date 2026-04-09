package eu.europeana.api.dataset.generation.service.impl;

import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.oaipmh.model.Record;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A {@code ZipRecordSink} provides functionality to write OAI-PMH records into a ZIP archive.
 * It implements the {@link RecordSink} interface for consuming individual records and the
 * {@link Closeable} interface for resource management.
 *
 * Each record is serialized to an XML string and added as a new entry in the ZIP file.
 * Record identifiers are used to generate unique entry names. If an identifier is not
 * available, a sequential name is assigned.
 *
 * This class is suitable for processing large datasets, as it streams records into the ZIP
 * archive incrementally, avoiding high memory consumption.
 *
 */
public class ZipRecordSink implements RecordSink, Closeable {

    private static final Logger LOG = LogManager.getLogger(ZipRecordSink.class);

    private final String datasetId;
    private final ZipOutputStream zipOut;
    private final DataFormatter formatter;
    private long counter = 0;

    public ZipRecordSink(String datasetId, File outputFile, DataFormatter formatter) throws IOException {
        this.datasetId = datasetId;
        this.zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        this.formatter = formatter;

    }

    @Override
    public void consume(Record record) throws IOException {
        String recordId = record.getIdentifier();
        String entryName = (recordId != null ? getDirectoryName(recordId) : "record_" + counter) + formatter.getFileExtension();

        ZipEntry entry = new ZipEntry(entryName);
        zipOut.putNextEntry(entry);
        zipOut.write(formatter.format(record.metadataStream).readAllBytes());
        zipOut.closeEntry();

        counter++;

        if (LOG.isDebugEnabled()) {
            if (counter % 1000 == 0) {
                LOG.info("Set : {} -  Written {} records to {} zip ", datasetId,  counter, formatter.getFileExtension());
            }
        }
    }

    public static String getDirectoryName(String identifier) {
       return StringUtils.substringAfterLast(identifier, "/");
    }

    @Override
    public void close() throws IOException {
        zipOut.close();
    }
}