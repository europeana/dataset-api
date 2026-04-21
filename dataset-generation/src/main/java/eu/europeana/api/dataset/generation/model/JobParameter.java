package eu.europeana.api.dataset.generation.model;

/** Parameters for triggering jobs */
public enum JobParameter {
  CURRENT_START_TIME("currentStartTime"),
  DATASET_ID("datasetId");

  private final String key;

  JobParameter(String key) {
    this.key = key;
  }

  /**
   * Retrieves the key associated with the job parameter.
   *
   * @return the key representing the job parameter
   */
  public String key() {
    return key;
  }
}
