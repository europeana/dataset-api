package eu.europeana.api.dataset.generation.model;

/** Parameters for triggering jobs */
public enum JobParameter {
  CURRENT_START_TIME("currentStartTime"),
  DATASET_ID("datasetId");

  private final String key;

  JobParameter(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
