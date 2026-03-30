package eu.europeana.api.dataset.generation.config.batch;

import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.listener.ScheduledDatasetItemListener;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.processor.DatasetReportTasklet;
import eu.europeana.api.dataset.generation.processor.OaiPmhZipProcessor;
import eu.europeana.api.dataset.generation.processor.DatasetDeletionTasklet;
import eu.europeana.api.dataset.generation.writer.ScheduledDatasetWriter;
import jakarta.annotation.Resource;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.ScopeConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskExecutor;

import org.springframework.transaction.PlatformTransactionManager;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.*;

/**
 * DatasetBatchConfig is a Spring configuration class responsible for defining and
 * managing the batch job and step beans for dataset generation tasks. This includes
 * configuring tasklets, step executors, transactions, and job repositories to orchestrate
 * the batch processing flow.
 *
 * It integrates the execution of dataset downloading tasks and allows for extensibility
 * for future batch processing requirements.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Configuration(DATASET_GENERATION_JOB_FACTORY)
@Import(ScopeConfiguration.class)
public class DatasetBatchConfig {

    /** SkipPolicy to ignore all failures when executing jobs, as they can be handled later */
    private static final SkipPolicy NOOP_SKIP_POLICY = (Throwable t, long skipCount) -> true;
    public static String JOB_DOWNLOAD_SCHEDULED_DATASETS = "download-scheduled-dataset-job";

    @Resource
    GeneratorSettings settings;

    @Autowired
    ApplicationContext appContext;

    @Resource
    ScheduledDatasetItemListener itemListener;

    @Resource
    PlatformTransactionManager transactionManager;

    @Resource
    JobRepository jobRepository;


    @Bean
    public Job createScheduledDownloadJob() {
        return new JobBuilder(JOB_DOWNLOAD_SCHEDULED_DATASETS, jobRepository)
                .start(datasetGenerationStep())
                .next(removeDatasets())
                .next(updateAndSendReport())
                .build();
    }


    /**
     * Configures and returns a Spring Batch {@link Step} for generating datasets.
     * This step processes scheduled datasets by reading their information from a data source,
     * applying processing logic, and writing the results. It also includes features such as
     * fault tolerance, skipping policies, and parallel task execution to ensure robustness and efficiency.
     *
     * @return a configured {@link Step} instance for the dataset generation process,
     *         including reading, processing, writing, and fault tolerance capabilities.
     */
    @Bean
    Step datasetGenerationStep() {
        return new StepBuilder("downloadDataset", jobRepository)
                .<ScheduledDataset, ScheduledDataset>chunk(settings.getBatchChunkSize(), transactionManager)
                .reader(getReader())
                .processor(getProcessor())
                .writer(getWriter()) // will write at the end status reporting and updating the processed datets in DB
                .listener((ItemProcessListener<? super ScheduledDataset, ? super ScheduledDataset>) itemListener)
                .faultTolerant()
                .skipPolicy(NOOP_SKIP_POLICY)
                .taskExecutor(getTaskExecutor())
                .throttleLimit(settings.getBatchUpdatesThrottleLimit())
                .build();

    }

    /**
     * Configures and returns a Spring Batch {@link Step} for updating the dataset status
     * and sending a corresponding report. This step utilizes a {@link DatasetReportTasklet},
     * which performs the following operations:
     *
     * - Updates the last harvest date to the current date in the database.
     * - Deletes processed datasets to free up resources and maintain state consistency..
     *
     * @return a configured {@link Step} instance for updating the dataset status
     *         and sending a report.
     */
    @Bean
    public Step updateAndSendReport() {
        return new StepBuilder("updateDatasetStatus", jobRepository)
                .tasklet(appContext.getBean(DatasetReportTasklet.class), transactionManager)
                .build();
    }

    /**
     * Configures and returns a Spring Batch {@link Step} for deleting outdated datasets.
     * This step uses a {@link DatasetDeletionTasklet} to identify and remove datasets
     * that are no longer part of the current dataset repository.
     *
     * The configured step will:
     * - Load a snapshot of previously existing dataset identifiers.
     * - Compare the snapshot with the current datasets returned by the search API.
     * - Identify and delete datasets that are no longer present in the current collection.
     * - Save the current dataset state as a new snapshot for future runs.
     *
     * @return a {@link Step} configured for dataset removal using a {@link DatasetDeletionTasklet}.
     */
    @Bean
    public Step removeDatasets() {
        return new StepBuilder("removeDatasets", jobRepository)
                .tasklet(appContext.getBean(DatasetDeletionTasklet.class), transactionManager)
                .build();
    }

    private SynchronizedItemStreamReader<ScheduledDataset> getReader() {
        return (SynchronizedItemStreamReader<ScheduledDataset>) appContext.getBean(SCHEDULED_DATASET_READER);
    }

    private ItemProcessor<ScheduledDataset, ScheduledDataset> getProcessor() {
        return appContext.getBean(OaiPmhZipProcessor.class);
    }

    private ItemWriter<ScheduledDataset> getWriter() {
        return appContext.getBean(ScheduledDatasetWriter.class);
    }

    private TaskExecutor getTaskExecutor() {
        return appContext.getBean(DATASET_GENERATION_STEP_EXECUTOR, TaskExecutor.class);
    }
}
