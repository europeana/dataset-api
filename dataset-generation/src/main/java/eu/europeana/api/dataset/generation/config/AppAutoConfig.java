package eu.europeana.api.dataset.generation.config;

import eu.europeana.api.commons_sb3.auth.AuthenticationBuilder;
import eu.europeana.api.commons_sb3.auth.AuthenticationConfig;
import eu.europeana.api.commons_sb3.auth.AuthenticationHandler;
import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.commons_sb3.slack.SlackConnection;
import eu.europeana.api.dataset.generation.deletion.impl.FileDeletionService;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.format.impl.TurtleFormatter;
import eu.europeana.api.dataset.generation.format.impl.XMLFormatter;
import eu.europeana.api.dataset.generation.listener.DatasetReportListener;
import eu.europeana.api.dataset.generation.listener.ScheduledDatasetItemListener;
import eu.europeana.api.dataset.generation.model.JobParameter;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.processor.DatasetDeletionTasklet;
import eu.europeana.api.dataset.generation.processor.TaskletSupport;
import eu.europeana.api.dataset.generation.reader.ScheduledDatasetDbReaderJdbc;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import eu.europeana.api.dataset.generation.writer.ScheduledDatasetWriter;
import eu.europeana.api.dataset.oaipmh.OAIPMHServiceClient;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    /**
     * Creates a StepScope bean that provides a {@link JdbcPagingItemReader} for reading
     * {@link ScheduledDataset} entities from the database in a paginated manner. This method
     * uses the batch chunk size from the application settings and initializes the reader
     * with the specified data source and job parameter for the current start time.
     *
     * @param dataSource the {@link DataSource} to use for database connections.
     * @param currentStartTime the current start time for the job, injected from job parameters.
     * @return a configured {@link JdbcPagingItemReader} for reading {@link ScheduledDataset} records.
     */
    @StepScope
    @Bean(name = SCHEDULED_DATASET_READER)
    public JdbcPagingItemReader<ScheduledDataset> reader(DataSource dataSource,
            @Value("#{jobParameters[currentStartTime]}") Instant currentStartTime) throws EuropeanaApiException {

       ScheduledDatasetDbReaderJdbc reader =  new ScheduledDatasetDbReaderJdbc(
                settings.getBatchChunkSize(),
                dataSource, currentStartTime);

        // set the JDBC setters
        reader.setDataSource(dataSource);
        // ✅ Correct mapping using aliases
        reader.setRowMapper(new BeanPropertyRowMapper<>(ScheduledDataset.class));
        reader.setQueryProvider();

        // ✅ Parameter binding
        Map<String, Object> params = new HashMap<>();
        params.put(JobParameter.CURRENT_START_TIME.key(), currentStartTime);
        reader.setParameterValues(params);

        LOG.info("ScheduledDatasetDbReaderJdbc initialized ..  !!");

        return  reader;
    }

    /**
     * Configures and provides a {@link PlatformTransactionManager} bean, which
     * serves as the central interface for handling transaction management within
     * the application. This implementation utilizes {@link JpaTransactionManager}
     * to manage transactions for JPA-based persistence.
     *
     * @return a configured {@link PlatformTransactionManager} instance for transaction management
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JpaTransactionManager();
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
                getStatusReportCsvPath(),
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

    /**
     * Provides a {@link DatasetReportListener} bean configured with application-specific
     * settings. The listener is responsible for handling dataset processing reports
     * during the application's operation.
     *
     * @return an instance of {@link DatasetReportListener}
     */
    @Bean
    public DatasetReportListener getdDatasetReportListener() {
        return new DatasetReportListener(
                settings.isForceHarvest(),
                TaskletSupport.getLastHarvestDate(settings.getLastHarvestDateFile()),
                settings.getSnapshotFile(),
                settings.getDatasetsFolder());
    }

    /**
     * Configures and provides an {@link ItemWriter} specifically for writing {@link ScheduledDataset}
     * entities to a flat CSV file. The writer is initialized with necessary settings, including the
     * header, file name, and line aggregation mechanism.
     *
     * @return an instance of {@link ItemWriter} that writes {@link ScheduledDataset} data to a flat file.
     */
    @Bean(SCHEDULED_DATASET_WRITER)
    public ItemWriter<ScheduledDataset> getFlatFileItemWriter() {
        FlatFileItemWriter<ScheduledDataset> writer = new FlatFileItemWriter<>();

        writer.setName("DatasetStatusReport");
        writer.setResource(new FileSystemResource(getStatusReportCsvPath()));
        writer.setAppendAllowed(true);

        writer.setHeaderCallback(w -> w.write(CSV_REPORT_HEADER));
        writer.setLineAggregator(lineAggregator(scheduledDatasetFieldExtractor()));

        return new ScheduledDatasetWriter(writer, getdDatasetReportListener());
    }

    /**
     * Provides a {@link FieldExtractor} for extracting specific fields from a {@link ScheduledDataset} instance.
     * The fields extracted include:
     * - Dataset ID
     * - Status
     * - Total size
     * - Failed records
     *
     * @return an instance of {@link FieldExtractor} that extracts an array of objects representing the dataset's fields.
     */
    @Bean
    public FieldExtractor<ScheduledDataset> scheduledDatasetFieldExtractor() {
        return dataset -> new Object[] {
                dataset.getDatasetId(),
                dataset.getStatus(),
                dataset.getTotalSize(),
                dataset.getFailedRecords()
        };
    }

    @Bean
    public DelimitedLineAggregator<ScheduledDataset> lineAggregator(
            FieldExtractor<ScheduledDataset> scheduledDatasetFieldExtractor) {

        DelimitedLineAggregator<ScheduledDataset> aggregator = new DelimitedLineAggregator<>();
        aggregator.setDelimiter(",");
        aggregator.setFieldExtractor(scheduledDatasetFieldExtractor);

        return aggregator;
    }


    /**
     * Generates an absolute file path for a CSV report within the specified directory.
     * The method ensures the generated file name does not overwrite any existing file
     * by appending an incremented counter if a file with the same base name exists.
     *
     * @return the absolute path of the generated CSV report file.
     */
    @Bean(STATUS_REPORT_CSV_PATH_BEAN)
    public String getStatusReportCsvPath() {
        String baseName = "status_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String extension = ".csv";

        File file = new File(settings.getDatasetsFolder() + baseName + extension);

        int counter = 1;
        while (file.exists()) {
            file = new File(settings.getDatasetsFolder() + baseName + "_" + counter + extension);
            counter++;
        }

        LOG.info("Status report CSV file path: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

}
