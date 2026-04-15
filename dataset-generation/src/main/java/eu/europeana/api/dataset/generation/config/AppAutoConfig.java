package eu.europeana.api.dataset.generation.config;

import eu.europeana.api.commons_sb3.auth.AuthenticationBuilder;
import eu.europeana.api.commons_sb3.auth.AuthenticationConfig;
import eu.europeana.api.commons_sb3.auth.AuthenticationHandler;
import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.commons_sb3.slack.SlackConnection;
import eu.europeana.api.dataset.generation.deletion.impl.FileDeletionService;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.format.impl.TurtleFormatter;
import eu.europeana.api.dataset.generation.format.impl.XMLFormatter;
import eu.europeana.api.dataset.generation.listener.ScheduledDatasetItemListener;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.processor.DatasetDeletionTasklet;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.reader.ScheduledDatasetDbReader;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import eu.europeana.api.dataset.oaipmh.OAIPMHServiceClient;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import javax.xml.transform.TransformerFactory;
import java.util.Map;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.*;

/**
 * AppAutoConfig is a Spring configuration class responsible for creating beans and
 * managing authentication handlers that facilitate access to other services like
 * Keycloak and Search Record API (SR API).
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Configuration
public class AppAutoConfig {

    private static final Logger LOG = LogManager.getLogger(AppAutoConfig.class);

    @Resource
    private GeneratorSettings settings;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * Generate AuthenticationHandler to access other services like SR API
     * @return
     */
    public AuthenticationHandler getAuthenticationHandler() {
        if (StringUtils.isNotEmpty(settings.getTokenEndpoint()) && StringUtils.isNotEmpty(settings.getKeycloakAccessGrantParams())) {
            AuthenticationConfig config = new AuthenticationConfig(settings.getTokenEndpoint(), settings.getKeycloakAccessGrantParams());
            return AuthenticationBuilder.newAuthentication(config);
        } else {
            LOG.error("Keycloak token endpoint and parameters NOT set !!");
        }
        return null;
    }

    /**
     * Creates an authentication handler for SR API access
     * @return authentication for SR API access
     */
    @Bean(SEARCH_RECORD_AUTH_HANDLER)
    public AuthenticationHandler getSearchRecordAccess() {
        return getAuthenticationHandler();
    }

    /**
     * Creates an authentication handler for SR API access
     * @return authentication for SR API access
     */
    @Bean(BEAN_OAI_PMH_CLIENT)
    public OAIPMHServiceClient getOaipmhClient() {
        return new OAIPMHServiceClient();
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

    /**
     *  By specifying a spring batch component being StepScope means that Spring Batch
     *  will use the spring container to instantiate a new instance of that component for each step execution.
     *
     *  Another useful reason to use StepScope is when you decide to reuse the same component in parallel steps
     */

    @Bean
    @StepScope
    public ScheduledDatasetItemListener getScheduledDatasetItemListener() {
        return new ScheduledDatasetItemListener(applicationContext.getBean(ScheduleDatasetService.class));
    }

    // todo update the query
//    @StepScope
    @Bean(name = SCHEDULED_DATASET_READER)
    public JpaPagingItemReader<ScheduledDataset> reader(EntityManagerFactory emf) {

        ScheduledDatasetDbReader reader = new ScheduledDatasetDbReader(settings.getBatchChunkSize(), emf, """
        SELECT s
        FROM ScheduledDataset s
       ORDER BY s.totalSize DESC, s.created ASC, s.id ASC
    """);

//        reader.setEntityManagerFactory(emf);
//
//        reader.setQueryString("""
//        SELECT s
//        FROM ScheduledDataset s
//       ORDER BY s.totalSize DESC, s.created ASC, s.id ASC
//    """);
//
//        reader.setPageSize(settings.getBatchChunkSize()); // chunk size match recommended

        return reader;
//        return  threadSafeReader(reader);
    }


//    @Bean(name = SCHEDULED_DATASET_READER)
//    @StepScope
//    public SynchronizedItemStreamReader<ScheduledDataset> scheduledTaskReader(
//            EntityManagerFactory emf,
//            @Value("#{jobParameters[currentStartTime]}") Date currentStartTime) {
//        ScheduledDatasetDbReader reader =
//                new ScheduledDatasetDbReader(
//                        settings.getBatchChunkSize(),
//                        """
//        SELECT s
//        FROM ScheduledDataset s
//        ORDER BY s.totalSize DESC, s.created ASC
//    """
//                        );
//        return threadSafeReader(reader);
//    }
//

    /** Makes ItemReader thread-safe */
    private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
        final SynchronizedItemStreamReader<T> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(reader);
        return synchronizedItemStreamReader;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new  ResourcelessTransactionManager();
    }

    @Bean
    public FileDeletionService getFileDeletionService() {
        return new FileDeletionService(settings.getDatasetsFolder());
    }

    // This will be a stepScope bean if we implement other storages in the future
    @Bean
    public DatasetDeletionTasklet getDeletionTasklet() {
        return  new DatasetDeletionTasklet(
                settings.getSnapshotFile(),
                applicationContext.getBean(SearchApiDatasetReader.class),
                new FileDeletionService(settings.getDatasetsFolder())) ;
    }

    @Bean(name = SLACK_CONNECTION_BEAN)
    public SlackConnection getSlackConnection() {
        return new SlackConnection(settings.getSlackWebhook());
    }

    /**
     * TransformerFactory.newInstance() is thread safe,
     *  one instance can be used for each formatter
     * @return
     */
    @Bean(name = DATA_FORMATS_BEAN)
    public Map<RdfFormat, DataFormatter> getFormats() {
        return Map.of(
                RdfFormat.XML, new XMLFormatter(TransformerFactory.newInstance()),
                RdfFormat.TURTLE, new TurtleFormatter(TransformerFactory.newInstance()));
    }


    /**
     * Initializes the Spring Batch metadata schema by explicitly loading the schema-h2.sql file
     * to populate the database schema for batch processing.
     *
     * NOTE: somehow Spring Batch metadata schema: org/springframework/batch/core/schema-h2.sql
     *        is not being loaded, forcefully loading it now
     *
     * @param dataSource the DataSource to which the batch schema will be applied
     * @return a DataSourceInitializer configured to populate the Spring Batch metadata schema
     */
    @Bean
    public DataSourceInitializer batchSchemaInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
