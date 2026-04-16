package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.repository.ScheduledDatasetRepository;
import jakarta.transaction.Transactional;
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
//    @Transactional
    public void scheduleDatasetsForDownload(List<Dataset> datasetsToUpdate) {
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
        LOGGER.info("Scheduled datasets - {}",data.stream().map(ScheduledDataset::getDatasetId).toList());
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

    public void markDatasetAsProcessed(List<ScheduledDataset> datasets) {
        List<ScheduledDataset> existingDatasets = repository.findAllById(datasets.stream().map(ScheduledDataset::getDatasetId).toList());

        List<ScheduledDataset> toSave = new ArrayList<>();

        for (ScheduledDataset dataset : existingDatasets) {
            dataset.setHasBeenProcessed(true);
            toSave.add(dataset);
        }
        repository.saveAll(toSave);
    }
}