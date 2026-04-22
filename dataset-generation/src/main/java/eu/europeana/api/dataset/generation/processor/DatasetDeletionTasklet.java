package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.deletion.DeletionService;
import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The DatasetDeletionTasklet class is responsible for performing a cleanup task
 * that identifies and deletes datasets that are no longer present.
 * It utilizes a snapshot mechanism to track datasets between task executions.
 * This class implements the Tasklet interface, allowing it to be used as a step
 * in a batch processing system.
 *
 * @author  Srishti Singh
 * @since   23 march 2026
 */
public class DatasetDeletionTasklet extends TaskletSupport implements Tasklet {

    private static final Logger LOG = LogManager.getLogger(DatasetDeletionTasklet.class);

    private final String snapshotFilePath;
    private final String csvReportPath;
    private final SearchApiDatasetReader searchApiDatasetReader;
    private final DeletionService deletionService;

    /**
     * Constructs a new instance of the DatasetDeletionTasklet.
     *
     * @param snapshotFilePath the file path where the snapshot of dataset identifiers
     *                         is stored. This file is used to determine which datasets
     *                         need to be deleted.
     *
     * @param csvReportPath the file path where the CSV report of deleted datasets is stored.
     *
     * @param searchApiDatasetReader an instance of SearchApiDatasetReader, which is used
     *                                to retrieve the current datasets available through the
     *                                search API.
     * @param deletionService an instance of DeletionService, which handles the deletion
     *                        of files associated with datasets that need to be removed.
     */
    public DatasetDeletionTasklet(String snapshotFilePath, String csvReportPath,
                                  SearchApiDatasetReader searchApiDatasetReader,
                                  DeletionService deletionService) {
        this.snapshotFilePath = snapshotFilePath;
        this.csvReportPath = csvReportPath;
        this.searchApiDatasetReader = searchApiDatasetReader;
        this.deletionService = deletionService;
    }

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOG.info("Starting dataset cleanup/deletion tasklet...");
        long start = System.currentTimeMillis();

        Set<String> previousSnapshot = loadSnapshot(this.snapshotFilePath);
        LOG.info("Loaded {} IDs from previous snapshot in {} ms", previousSnapshot.size(),
                System.currentTimeMillis() - start);

        Set<String> currentDatasets = getCurrentDatasets();
        LOG.info("Fetched {} current datasets from SR Api", currentDatasets.size());

        Set<String> datasetsForRemoval = getDatasetsForRemoval(previousSnapshot, currentDatasets);
        LOG.info("Removing datasets: {}", datasetsForRemoval);

        // delete the datasets
        deletionService.deleteFiles(datasetsForRemoval);

        // add the removed datasets in the csv report
        addDeletedDatasetToReport(csvReportPath, datasetsForRemoval);

        // Save the current snapshot for the next run
        saveSnapshot(currentDatasets);
        LOG.info("Dataset cleanup tasklet finished");

        return RepeatStatus.FINISHED;
    }

    /**
     * Identifies datasets that need to be removed by determining the difference
     * between a previous snapshot of dataset identifiers and the current set of
     * dataset identifiers.
     *
     * @param previousSnapshot the set of dataset identifiers from the previous snapshot
     * @param currentDatasets the set of currently available dataset identifiers
     * @return a set of dataset identifiers that are present in the previous snapshot
     * but not in the current set (i.e., datasets to be removed)
     */
    private Set<String> getDatasetsForRemoval(Set<String> previousSnapshot, Set<String> currentDatasets ) {
        Set<String> datasetsForRemoval = new HashSet<>(previousSnapshot);
        datasetsForRemoval.removeAll(currentDatasets);
        return datasetsForRemoval;
    }


    /**
     * Retrieves the set of current dataset identifiers by reading datasets
     * from the Search API and collecting their identifiers.
     *
     * @return a set of dataset identifiers currently available in the Search API.
     * @throws EuropeanaApiException if an error occurs while interacting with the Search API.
     */
    private Set<String> getCurrentDatasets() throws EuropeanaApiException {
      return searchApiDatasetReader.getDataset(null).stream()
                .map(Dataset::getDatasetId)
                .collect(Collectors.toSet());
    }

    /**
     * Saves a snapshot of dataset identifiers to a file.
     * @param data
     * @throws IOException
     */
    private void saveSnapshot(Set<String> data) throws IOException {
        Files.write(Paths.get(this.snapshotFilePath), data);
    }
}
