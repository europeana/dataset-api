package eu.europeana.api.dataset.generation.model;

/**
 * Represents a dataset with an associated identifier and size.
 * This class provides methods to access and modify the dataset's properties.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
public class Dataset {

    private String datasetId;

    private long datasetSize;

    public String getDatasetId() {
        return datasetId;
    }

    public Dataset(String datasetId, long datasetSize) {
        this.datasetId = datasetId;
        this.datasetSize = datasetSize;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public long getDatasetSize() {
        return datasetSize;
    }

    public void setDatasetSize(long datasetSize) {
        this.datasetSize = datasetSize;
    }
}
