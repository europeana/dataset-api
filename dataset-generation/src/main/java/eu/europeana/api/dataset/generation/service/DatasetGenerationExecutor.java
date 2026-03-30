package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.config.batch.DatasetBatchConfig;
import eu.europeana.api.dataset.generation.model.JobParameter;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.Date;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.DATASET_GENERATION_JOB_FACTORY;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.DATASET_GENERATION_JOB_LAUNCHER;

/**
 * The DatasetGenerationExecutor class is responsible for orchestrating the execution
 * of dataset generation jobs in a scheduled and asynchronous manner.
 * @author Srishti Singh
 * @since 25 Feb 2026
 */
@Service
public class DatasetGenerationExecutor {

    private static final Logger logger = LogManager.getLogger(DatasetGenerationExecutor.class);

    private final DatasetBatchConfig datasetBatchConfig;
    private final JobLauncher datasetDownloadJobLauncher;

    public DatasetGenerationExecutor(
            @Qualifier(DATASET_GENERATION_JOB_FACTORY) DatasetBatchConfig datasetBatchConfig,
            @Qualifier(DATASET_GENERATION_JOB_LAUNCHER) JobLauncher datasetDownloadJobLauncher) {
        this.datasetBatchConfig         = datasetBatchConfig;
        this.datasetDownloadJobLauncher = datasetDownloadJobLauncher;
    }

    /** Periodically run scheduled datasets (in one run). */
    @Async
    public void runScheduleDatasets() {
        logger.info("Running scheduled datasets....");
        try {
            datasetDownloadJobLauncher.run(
                    datasetBatchConfig.createScheduledDownloadJob(),
                    createJobParameters(null, Date.from(Instant.now())));

        } catch (Exception e) {
            logger.warn("Error running scheduled datasets", e);
        }
    }

    /**
     * Creates JobParameters for triggering the Spring Batch update job for specific entities
     *
     * @param datasetId dataset id
     * @param runTime trigger time for a job
     * @return JobParameters with trigger time and entityId
     */
    public static JobParameters createJobParameters(@Nullable String datasetId, Date runTime) {
        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                .addDate(JobParameter.CURRENT_START_TIME.key(), runTime);

        if (StringUtils.hasLength(datasetId)) {
            jobParametersBuilder.addString(JobParameter.DATASET_ID.key(), datasetId);
        }

        return jobParametersBuilder.toJobParameters();
    }
}
