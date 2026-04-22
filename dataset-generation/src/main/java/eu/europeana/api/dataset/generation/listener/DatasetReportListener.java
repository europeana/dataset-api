package eu.europeana.api.dataset.generation.listener;

import eu.europeana.api.dataset.generation.model.DatasetStatus;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.processor.TaskletSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Set;

import static eu.europeana.api.dataset.generation.model.DatasetStatus.*;

/**
 * The {@code DatasetReportListener} class is responsible for managing the status of datasets
 * during a harvesting process. It evaluates whether datasets are new, changed, unchanged,
 * reharvested, or deleted based on their existence, modification date, and additional parameters.
 * It also logs the dataset processing status and generates reports.
 */
public class DatasetReportListener extends TaskletSupport   {

    private static final Logger LOG = LogManager.getLogger(DatasetReportListener.class);

    private final Set<String> previousDatasetIds;
    private final boolean forceReharvest;
    private final Date lastHarvestDate;
    private final String datasetFolder;

    /**
     * Constructs a new {@code DatasetReportListener} instance with the provided configuration.
     * The listener is responsible for managing dataset reports and performing operations such
     * as determining the status of datasets during the processing workflow.
     *
     * @param forceReharvest a boolean flag indicating whether all datasets should be reharvested
     *                       regardless of their changes.
     * @param lastHarvestDate the date of the last harvest operation. This value can be used
     *                        to determine datasets that have been modified since the last harvest.
     * @param snapshotFilePath the file path to the snapshot containing previously harvested dataset
     *                         identifiers. These identifiers will be loaded for comparison with
     *                         the current state.
     * @param datasetFolder the directory path where dataset files are stored and processed.
     */
    public DatasetReportListener(boolean forceReharvest, Date lastHarvestDate, String snapshotFilePath, String datasetFolder ) {
        this.forceReharvest  = forceReharvest;
        this.datasetFolder   = datasetFolder;
        this.lastHarvestDate = lastHarvestDate;
        this.previousDatasetIds = loadSnapshot(snapshotFilePath);

        LOG.info("DatasetReportListener initialized ...!! " +
                "forceHarvest {}, lastHarvestDate {}, Loaded {} IDs from previous snapshot",
                forceReharvest, lastHarvestDate, previousDatasetIds.size());
    }

    /**
     * Accumulates the status of a scheduled dataset by determining whether it is new, unchanged,
     * changed, reharvested, or deleted. The determination is made based on the dataset's metadata,
     * previous state, and the reharvest requirement.
     *
     * @param dataset the scheduled dataset whose status is to be determined. It contains the dataset ID
     *                and associated metadata necessary for processing.
     * @return the {@code DatasetStatus} representing the current state of the dataset. This status indicates
     *         whether the dataset is new, unchanged, changed, reharvested, or deleted.
     */
    public DatasetStatus accumulate(ScheduledDataset dataset) {
        FileMetadata metadata = extractFileMetadata(datasetFolder + "XML/" + dataset.getDatasetId() + ".zip");

        return determineStatus(previousDatasetIds.contains(dataset.getDatasetId()),
                metadata.getModifiedDate(),
                lastHarvestDate,
                forceReharvest);
    }

    /**
     * Determines the status of a dataset based on its previous existence, modification date,
     * last harvested date, and whether reharvesting is forced.
     * See : {@link https://europeana.atlassian.net/browse/EA-3908}
     *
     * @param existedBefore indicates whether the dataset existed prior to the current operation.
     * @param modifiedDate the date when the dataset was last modified.
     * @param lastHarvestedOn the date when the dataset was last harvested.
     * @param forceReharvest a flag indicating whether the dataset should be reharvested regardless
     *                       of its modification state.
     * @return the {@code DatasetStatus} representing the state of the dataset. Possible values
     *         include {@code NEW}, {@code UNCHANGED}, {@code CHANGED}, {@code REHARVESTED}, or
     *         {@code DELETED}.
     */
    public static DatasetStatus determineStatus(boolean existedBefore, Date modifiedDate, Date lastHarvestedOn, boolean forceReharvest) {
        if (!existedBefore) {
            return NEW;
        }

        boolean isUnchanged = (modifiedDate.before(lastHarvestedOn)) ;
        if (isUnchanged && forceReharvest) {
            return UNCHANGED;
        }

        boolean reharvested = modifiedDate.after(lastHarvestedOn) || modifiedDate.equals(lastHarvestedOn);
        if (reharvested && forceReharvest) {
            return REHARVESTED;
        }

        return CHANGED;
    }

    /**
     * Extracts metadata from a specified file, including creation and last modified dates.
     * This method uses basic file attributes to retrieve the necessary metadata and wraps
     * it into a {@code FileMetadata} object.
     *
     * @param fileName the name of the file for which metadata is to be extracted. It should
     *                 include the complete file path if the file is located outside the
     *                 current working directory.
     * @return a {@code FileMetadata} object containing the creation date and last modified
     *         date of the specified file.
     * @throws IllegalStateException if the file metadata could not be read or retrieved.
     */
    public FileMetadata extractFileMetadata(String fileName) {
        File file = new File(fileName);
        Path filePath = file.toPath();

        BasicFileAttributes attributes = getBasicFileAttributes(filePath);

        if (attributes != null) {
            return new FileMetadata(
                    new Date(attributes.creationTime().toMillis()),
                    new Date(attributes.lastModifiedTime().toMillis())
            );
        }

        throw new IllegalStateException("Unable to read file metadata for " + fileName);
    }


    public class FileMetadata {
        private final Date creationDate;
        private final Date modifiedDate;

        public FileMetadata(Date creationDate, Date modifiedDate) {
            this.creationDate = creationDate;
            this.modifiedDate = modifiedDate;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public Date getModifiedDate() {
            return modifiedDate;
        }
    }
}