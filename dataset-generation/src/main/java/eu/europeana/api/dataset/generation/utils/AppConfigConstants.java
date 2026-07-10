package eu.europeana.api.dataset.generation.utils;

public interface AppConfigConstants {

    // beans
    String BEAN_BATCH_SCHEDULED_DATASET_SERVICE  = "scheduledDatasetService";
    String DATASET_GENERATION_STEP_EXECUTOR      = "datasetGenerationStepExecutor";
    String DATASET_GENERATION_JOB_FACTORY        = "datasetGenerationJobFactory";
    String SEARCH_RECORD_AUTH_HANDLER            = "searchRecordAuthHandler";
    String BEAN_OAI_PMH_CLIENT                   = "oaipmhClient";

    String READER_FACTORY_BEAN                   = "readerFactory";
    String LIST_SCHEDULED_DATASET_READER         = "listScheduledDatasetReader";
    String JDBC_SCHEDULED_DATASET_READER         = "jdbcScheduledDatasetReader";
    String OAI_PMH_ZIP_PROCESSOR                 = "oaiPmhZipProcessor";
    String SCHEDULED_DATASET_WRITER              = "scheduledDatasetWriter";
    String SLACK_CONNECTION_BEAN                 = "slackConnection";
    String DATA_FORMATS_BEAN                     = "dataFormats";
    String STATUS_REPORT_CSV_PATH_BEAN           = "statusReportCsvPath";

    // other constants
    String CSV_REPORT_HEADER                     = "DatasetId,FileStatus,TotalRecords,FailedRecord";
}
