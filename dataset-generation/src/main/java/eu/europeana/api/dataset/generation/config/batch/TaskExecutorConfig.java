package eu.europeana.api.dataset.generation.config.batch;

import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.DATASET_GENERATION_STEP_EXECUTOR;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.SCHEDULED_GENERATION_TASK_EXECUTOR;

/**
 * TaskExecutorConfig is a Spring configuration class that defines and configures
 * multiple TaskExecutors for asynchronous task processing in the application.
 * These TaskExecutors are used for handling concurrent execution of tasks in
 * various contexts like scheduled dataset downloads and batch processing.
 *
 * @author  Srishti Singh
 * @since   23 Feb 2026
 */
@Configuration
public class TaskExecutorConfig {

  @Resource
  private GeneratorSettings settings;

  public TaskExecutorConfig() {
  }


  /**
   * Returns a TaskExecutor to be used for scheduled datasets download.
   * This is a singleThreadExecutor, so updates cannot run simultaneously.
   * Launch all scheduled jobs within the Spring scheduling thread
   * @return new SyncTaskExecutor
   */
  @Bean(SCHEDULED_GENERATION_TASK_EXECUTOR)
  public TaskExecutor scheduledUpdateExecutor() {
    return new SyncTaskExecutor();
  }

  /**
   * Executor used for Steps when running Scheduled datasets.
   * @return new ThreadedPoolTaskExecutor
   */
  @Bean(DATASET_GENERATION_STEP_EXECUTOR)
  public TaskExecutor datasetGenerationStepExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(settings.getBatchUpdatesCorePoolSize());
    taskExecutor.setMaxPoolSize(settings.getBatchUpdatesMaxPoolSize());
    taskExecutor.setQueueCapacity(settings.getBatchUpdatesQueueSize());

    return taskExecutor;
  }
}
