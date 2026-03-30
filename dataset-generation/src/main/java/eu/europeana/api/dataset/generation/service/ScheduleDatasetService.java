package eu.europeana.api.dataset.generation.service;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.query.filters.Filter;
import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.repository.ScheduledDatasetRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_BATCH_SCHEDULED_DATASET_SERVICE;

/**
 * This service handles the scheduling of datasets for download by creating
 * and persisting instances of {@link ScheduledDataset}.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Service(BEAN_BATCH_SCHEDULED_DATASET_SERVICE)
public class ScheduleDatasetService {

    private static final Logger LOGGER = LogManager.getLogger(ScheduleDatasetService.class);

    private final ScheduledDatasetRepository repository;

    @Autowired
    public ScheduleDatasetService(ScheduledDatasetRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates {@link ScheduledDataset} instances for dataset ids and their total size, and then saves
     * them to the database.
     *
     * @param datasetsToUpdate
     */
    public void scheduleDatasetsForDownload(List<Dataset> datasetsToUpdate) {
        List<ScheduledDataset> tasks = createScheduledTasks(datasetsToUpdate, false);

        BulkWriteResult writeResult = repository.upsertBulk(tasks);
        LOGGER.debug(
                "Persisted scheduled datasets to db: matched={}, modified={}, inserted={}",
                writeResult.getMatchedCount(),
                writeResult.getModifiedCount(),
                writeResult.getInsertedCount());
    }

    /**
     * Helper method to create {@link ScheduledDataset} instances for dataset ids and their total size.
     * @param datasetsToUpdate datasets to update
     * @param hasBeenProcessed with their processing status
     * @return List of scheduled datasets
     */
    private List<ScheduledDataset> createScheduledTasks(List<Dataset> datasetsToUpdate, boolean hasBeenProcessed) {
        Instant now = Instant.now();

        return datasetsToUpdate.stream()
                .map(
                        dataset ->
                                new ScheduledDataset.Builder(dataset.getDatasetId(), dataset.getDatasetSize())
                                        .setProcessed(hasBeenProcessed)
                                        .modified(now)
                                        .build())
                .collect(Collectors.toList());
    }


    public List<ScheduledDataset> getDatasets(
            int start, int count, Filter[] queryFilters) {
        return repository.getDatasets(start, count, queryFilters);
    }

    /**
     * Retrieves the count of scheduled datasets that are currently marked as "running"
     * (i.e., hasBeenProcessed is false).
     *
     * @return the number of scheduled datasets that are not marked as processed
     */
    public long getRunningTasksCount() {
        return repository.getRuningTasksCount();
    }

    /**
     * Marks entities as processed.
     *
     * @param scheduledDatasets
     */
    public void markAsProcessed(List<ScheduledDataset> scheduledDatasets) {
        BulkWriteResult writeResult = repository.markAsProcessed(scheduledDatasets);
        LOGGER.debug(
                "Marked scheduled datasets as processed: matched={}, modified={}, inserted={}",
                writeResult.getMatchedCount(),
                writeResult.getModifiedCount(),
                writeResult.getInsertedCount());
    }

    public void cleanUpAfterProcessing() {
        DeleteResult results = repository.cleanUpAfterProcessing();
        LOGGER.debug(
                "Datasets marked as processed have been deleted: deleted={}",
                results.getDeletedCount());
    }

    public Date getLatestHarvestDate() {
        return repository.getLastHarvestDate();
    }

    public void updateLastHarvestDate(Date lastHarvestDate) {
        LOGGER.info("Updating last harvest date to {}", lastHarvestDate);
        repository.updateLastHarvestDate(lastHarvestDate);
    }
}
