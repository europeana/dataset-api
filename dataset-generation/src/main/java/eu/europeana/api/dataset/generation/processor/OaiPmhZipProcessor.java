package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.model.RetryType;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ListRecordService;
import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import eu.europeana.api.dataset.generation.service.impl.MultiRecordSink;
import eu.europeana.api.dataset.generation.service.impl.ZipRecordSink;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.DATA_FORMATS_BEAN;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.OAI_PMH_ZIP_PROCESSOR;

/**
 * A processor for harvesting OAI-PMH records from a given dataset and storing them
 * as a ZIP archive. This class uses the {@link ListRecordService} to stream records
 * and writes them into a ZIP file using {@link ZipRecordSink}.
 *
 * This processor acts as the integration point between the record harvesting service
 * and the process of archiving records to disk. It ensures efficient handling of potentially
 * large datasets by using a streaming approach, avoiding memory overhead.
 *
 * @author Srishti Singh
 * @since 12 March 2026
 */
@Component(OAI_PMH_ZIP_PROCESSOR)
public class OaiPmhZipProcessor extends BaseProcessor {

    @Resource
    GeneratorSettings settings;

    @Resource
    private ListRecordService listRecordService;

    @Resource
    ScheduleDatasetService scheduleDatasetService;

    @Resource(name = DATA_FORMATS_BEAN)
    private Map<RdfFormat, DataFormatter> formats;

    @Override
    ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception {

        List<RecordSink> sinks = new ArrayList<>();
        List<Path> tempFiles = new ArrayList<>();
        List<Path> targetFiles = new ArrayList<>();

        for (var entry : formats.entrySet()) {
            Path dir = createFolder(entry.getKey()).toPath();

            Path target = dir.resolve(dataset.getDatasetId() + ".zip");
            Path temp = Files.createTempFile(dir, dataset.getDatasetId() + ".", ".zip.tmp");

            tempFiles.add(temp);
            targetFiles.add(target);

            sinks.add(new ZipRecordSink(dataset.getDatasetId(), temp.toFile(), entry.getValue()));
        }

        try (MultiRecordSink multiSink = new MultiRecordSink(sinks)) {
            // pass recordsPerPage only for the datasets coming for the retries
            String recordsPerPage = retryNeeded(dataset) ? settings.getRetryRecordsPerPage() : null;
            long failedRecords = listRecordService.streamRecords(dataset, multiSink, recordsPerPage);
            scheduleDatasetService.updateFailedRecords(dataset, failedRecords); // update failedRecords and hasBeenProcessed
        }

        // 🔥 Atomic swap happens ONLY after all sinks are closed
        // and if the dataset is harvested successfully
        if (scheduleDatasetService.isDatasetHarvestedSuccessfully(dataset)) {
            performAtomicSwap(tempFiles, targetFiles);
            createMd5File(targetFiles);
        }

        return dataset;
    }

    @Override
    ScheduledDataset doRetryProcessing(ScheduledDataset dataset) throws Exception {
        LOG.info("Dataset {} retry processing started for retry type {}",
                dataset.getDatasetId(),
                dataset.getRetryType());
       return doProcessing(dataset);
    }

    /**
     * Determines if a retry is needed for the given dataset based on its harvest status
     * and retry type.
     *
     * @param dataset the {@link ScheduledDataset} to check for retry necessity.
     * @return {@code true} if the dataset has not been successfully harvested and its retry type
     *         is {@code RetryType.LOWER_RECORDS_PER_PAGE}; {@code false} otherwise.
     */
    @Override
    boolean retryNeeded(ScheduledDataset dataset) {
        return !scheduleDatasetService.isDatasetHarvestedSuccessfully(dataset)
               && (dataset.getRetryType() != null && dataset.getRetryType().equals(RetryType.LOWER_RECORDS_PER_PAGE));
    }

    /**
     * Creates a new folder within the datasets directory if it does not already exist.
     *
     * @param rdfFormat  rdf format of the folder
     * @return the {@link File} object representing the folder
     */
    private File createFolder(RdfFormat rdfFormat) {
        File folder =  new File(settings.getDatasetsFolder(), getFolderName(rdfFormat));

        if (!folder.exists() ) {
            folder.mkdirs();
        }
        return  folder;
    }
}

