package eu.europeana.api.dataset.generation.service.impl;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
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
    public void consume(Record record) throws EuropeanaApiException {
        String recordId = record.getIdentifier();
        try {
            String entryName = (recordId != null ? getDirectoryName(recordId) : "record_" + counter) + formatter.getFileExtension();
            ZipEntry entry = new ZipEntry(entryName);
            zipOut.putNextEntry(entry);
            formatter.write(recordId, record.metadata, zipOut);
            zipOut.closeEntry();
            counter++;
        } catch (IOException e) {
            LOG.error("Error writing record {} to zip - {}", recordId ,  e.getMessage(), e);
            throw new EuropeanaApiException("Error writing record " + recordId + " to zip - "+ e.getMessage(), e);
        }

        if (LOG.isTraceEnabled()) {
            if (counter % 1000 == 0) {
                LOG.trace("Set : {} -  Written {} records to {} zip ", datasetId,  counter, formatter.getFileExtension());
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