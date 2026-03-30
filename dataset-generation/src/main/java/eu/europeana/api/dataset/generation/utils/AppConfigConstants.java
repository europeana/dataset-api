package eu.europeana.api.dataset.generation.utils;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;

public interface AppConfigConstants {

    // beans
    String BEAN_DATASET_DATA_STORE  = "datasetDatastore";
    String BEAN_BATCH_SCHEDULED_DATASET_SERVICE= "scheduledDatasetService";
    String DATASET_GENERATION_STEP_EXECUTOR = "datasetGenerationStepExecutor";
    String DATASET_GENERATION_JOB_LAUNCHER = "datasetGenerationJobLauncher";
    String BEAN_BATCH_MONGO_CONFIGURER = "batchMongoConfigurer";
    String DATASET_GENERATION_JOB_FACTORY = "datasetGenerationJobFactory";
    String SCHEDULED_GENERATION_TASK_EXECUTOR = "scheduledGenerationTaskExecutor";
    String SEARCH_RECORD_AUTH_HANDLER = "searchRecordAuthHandler";
    String BEAN_OAI_PMH_CLIENT = "oaipmhClient";

    String SCHEDULED_DATASET_READER = "scheduledDatasetReader";
    String OAI_PMH_ZIP_PROCESSOR = "oaiPmhZipProcessor";
    String SCHEDULED_DATASET_WRITER = "scheduledDatasetWriter";
    String SLACK_CONNECTION_BEAN    = "slackConnection";
    String DATA_FORMATS_BEAN        = "dataFormats";


    // Mongo constants
    String MORPHIA_DISCRIMINATOR = "_t";
    String SCHEDULED_DATASET_CLASSNAME = ScheduledDataset.class.getSimpleName();

    // Document ops
    String DOC_SET = "$set";
    String DOC_SET_ON_INSERT = "$setOnInsert";

}
