package eu.europeana.api.web;

import java.util.List;

/**
 * Response element to group the dataset and the files (e.g. ttl ,xml ,rdf) associated with it.
 * @author Shwetambara Nazare
 * &#064;Since  05 October 2026
 */
public class DatasetFileGroup {
    private String datasetName;
    private List<FileDetails> files;

    /* Initialize with dataset name and file details list */
    public DatasetFileGroup(String datasetName, List<FileDetails> files) {
        this.datasetName = datasetName;
        this.files = files;
    }

    // Getters and Setters
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
    public List<FileDetails> getFiles() { return files; }
    public void setFiles(List<FileDetails> files) { this.files = files; }
}
