package eu.europeana.api.dataset.generation.config.batch;

import dev.morphia.Datastore;
import eu.europeana.batch.config.MongoBatchConfigurer;
import jakarta.annotation.Resource;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.*;

/**
 * JobLauncherConfig is a Spring service class responsible for configuring
 * and providing beans required for managing Spring Batch jobs using a MongoDB backend.
 * It provides the necessary infrastructure for job execution, transaction management,
 * and custom job launchers.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Service
public class JobLauncherConfig {

    @Resource(name = BEAN_BATCH_MONGO_CONFIGURER)
    private MongoBatchConfigurer mongoBatchConfigurer;

    @Resource(name = SCHEDULED_GENERATION_TASK_EXECUTOR)
    private TaskExecutor defaultTaskExecutor;


    /**
     * Creates and configures a {@link MongoBatchConfigurer} bean for managing
     * batch processing with a MongoDB backend. This method specifies the datastore
     * and task executor to be used by the {@link MongoBatchConfigurer}.
     *
     * @param datastore the {@link Datastore} instance used to interact with the MongoDB database.
     * @return a fully configured instance of {@link MongoBatchConfigurer}.
     */
    @Bean(name = BEAN_BATCH_MONGO_CONFIGURER)
    public MongoBatchConfigurer mongoBatchConfigurer(
            @Qualifier(BEAN_DATASET_DATA_STORE) Datastore datastore) {
        return new MongoBatchConfigurer(datastore, defaultTaskExecutor);
    }

    /**
     * Creates and configures a custom {@link JobLauncher} bean for dataset generation jobs.
     * This method builds a default {@link JobLauncher} using the {@link MongoBatchConfigurer},
     * which provides the job repository and task executor required for job execution.
     *
     * @return a configured instance of {@link JobLauncher} specifically for dataset generation tasks
     * @throws Exception if there is an error during the creation of the {@link JobLauncher} instance
     */
    @Bean(name = DATASET_GENERATION_JOB_LAUNCHER)
    @Primary
    public JobLauncher datasetGenerationJoblauncher() throws Exception {
        return mongoBatchConfigurer.getJobLauncher();
    }

    /**
     * Required beans configured via {@link MongoBatchConfigurer}
     */
    @Bean
    public JobRepository jobRepository() throws Exception {
        return mongoBatchConfigurer.getJobRepository();
    }

    @Bean
    public PlatformTransactionManager getTransactionManager() throws Exception {
        return mongoBatchConfigurer.getTransactionManager();
    }
}
