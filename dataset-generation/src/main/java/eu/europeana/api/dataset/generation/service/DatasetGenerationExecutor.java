package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.model.JobParameter;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;

/**
 * The DatasetGenerationExecutor class is responsible for orchestrating the execution
 * of dataset generation jobs in a scheduled and asynchronous manner.
 * @author Srishti Singh
 * @since 25 Feb 2026
 */
@Service
public class DatasetGenerationExecutor {

    private static final Logger LOGGER = LogManager.getLogger(DatasetGenerationExecutor.class);

    private final Job scheduledDatasetJob;
    private final JobLauncher datasetDownloadJobLauncher;

    public DatasetGenerationExecutor(
            @Qualifier("createScheduledDownloadJob")Job scheduledDatasetJob,
            JobLauncher datasetDownloadJobLauncher) {
        this.scheduledDatasetJob         = scheduledDatasetJob;
        this.datasetDownloadJobLauncher = datasetDownloadJobLauncher;
    }


    @Async
    public void runScheduleDatasets() {
        LOGGER.info("Running scheduled datasets....");

        try {
            datasetDownloadJobLauncher.run(
                    scheduledDatasetJob,
                    createJobParameters(null, Date.from(Instant.now()))
            );
        } catch (Exception e) {
            LOGGER.warn("Error running scheduled datasets", e);
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
