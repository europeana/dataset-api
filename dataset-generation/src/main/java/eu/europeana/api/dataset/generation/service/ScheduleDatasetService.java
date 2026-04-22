package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.repository.ScheduledDatasetRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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

    /**
     * Constructor for the ScheduleDatasetService class.
     * @param repository the repository for accessing scheduled dataset data
     */
    @Autowired
    public ScheduleDatasetService(ScheduledDatasetRepository repository) {
        this.repository = repository;
    }

    /**
     * Schedules a list of datasets for downloading by creating or updating
     * records in the database based on the given dataset details.
     *
     * @param datasetsToUpdate A list of {@link Dataset} objects containing
     *                         the details of datasets to be scheduled for download.
     *                         If the list is null or empty, the method does nothing.
     */
    public void scheduleDatasetsForDownload(List<Dataset> datasetsToUpdate) {
        if (datasetsToUpdate == null || datasetsToUpdate.isEmpty()) {
            LOGGER.info("No datasets will be scheduled for download .... !!!");
            return;
        }
        // Fetch all existing records in ONE query
        List<ScheduledDataset> existingList = findAll(datasetsToUpdate);
        Map<String, ScheduledDataset> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        ScheduledDataset::getDatasetId,
                        e -> e
                ));

        Instant now = Instant.now();

        // Upsert
        List<ScheduledDataset> toSave = new ArrayList<>();

        for (Dataset dataset : datasetsToUpdate) {
            ScheduledDataset entity = existingMap.get(dataset.getDatasetId());
            if (entity == null) { // insert
                entity = new ScheduledDataset();
                entity.setDatasetId(dataset.getDatasetId());
                entity.setCreated(now);
                entity.setHasBeenProcessed(false);
            }
            // UPDATE (for both insert + update)
            entity.setTotalSize(dataset.getDatasetSize());
            entity.setModified(now);

            toSave.add(entity);
        }

        // 4. Single batch write to DB
        List<ScheduledDataset> data = repository.saveAllAndFlush(toSave);
        LOGGER.info("Scheduled {} datasets for download", toSave.size());
        LOGGER.info("Scheduled datasets - {}", data.stream().map(ScheduledDataset::getDatasetId).toList());
    }

    /**
     * The method extracts the dataset IDs from the given list and fetches their associated entities
     * from the repository in a single query.
     *
     * @param datasetsToUpdate A list of {@link Dataset} objects to be scheduled for download.
     * @return A list of {@link ScheduledDataset} objects retrieved from the repository. The list will
     *         contain entities matching the IDs of the provided datasets. If no matching entities are found,
     *         an empty list is returned.
     */
    private List<ScheduledDataset> findAll(List<Dataset> datasetsToUpdate) {
        List<String> ids = datasetsToUpdate.stream()
                .map(Dataset::getDatasetId)
                .toList();

       return repository.findAllById(ids);
    }

    /**
     * Marks the provided list of {@link ScheduledDataset} objects as processed by updating
     * their `hasBeenProcessed` status to true in the database.
     *
     * @param datasets A list of {@link ScheduledDataset} objects to be marked as processed.
     *                 Each dataset in the list should already exist in the repository.
     */
    public void markDatasetAsProcessed(List<ScheduledDataset> datasets) {
        List<ScheduledDataset> existingDatasets = repository.findAllById(datasets.stream().map(ScheduledDataset::getDatasetId).toList());

        List<ScheduledDataset> toSave = new ArrayList<>();

        for (ScheduledDataset dataset : existingDatasets) {
            dataset.setHasBeenProcessed(true);
            toSave.add(dataset);
        }
        List<ScheduledDataset> saved = repository.saveAllAndFlush(toSave);
        LOGGER.info("Marked {} datasets as processed", saved.size());
    }

    /**
     * Updates the number of failed records and the processed status of the given {@link ScheduledDataset}.
     * If the number of failed records is 0, the dataset is marked as processed.
     * The updated dataset is saved to the repository and a log entry is created.
     *
     * @param dataset the {@link ScheduledDataset} whose failed records and processed status are to be updated
     * @param failedRecords the number of failed records to set in the dataset
     */
    public void updateFailedRecords(ScheduledDataset dataset, long failedRecords) {
        dataset.setFailedRecords(failedRecords);
        dataset.setHasBeenProcessed(failedRecords == 0);
        ScheduledDataset saved = repository.saveAndFlush(dataset);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Dataset {} updated with failedRecords : {} , hasBeenProcessed : {}",
                    saved.getDatasetId(), saved.getFailedRecords(), saved.getHasBeenProcessed());
        }
    }

    public List<ScheduledDataset> findHasBeenProcessedFalse() {
        return repository.findByHasBeenProcessedFalse();
    }

    public void updateStatus(List<ScheduledDataset> datasets) {
        repository.saveAllAndFlush(datasets);
    }

    public long count() {
       return repository.count();
    }
}
