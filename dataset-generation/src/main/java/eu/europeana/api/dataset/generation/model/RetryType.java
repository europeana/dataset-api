package eu.europeana.api.dataset.generation.model;

public enum RetryType {

    LOWER_RECORDS_PER_PAGE, // retry with lower records per page
    DEFAULT;               // simple retry
}
