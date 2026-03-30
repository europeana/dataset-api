package eu.europeana.api.dataset.generation.config;

import dev.morphia.query.filters.Filters;
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
import eu.europeana.api.dataset.generation.reader.batch.ScheduledDatasetDbReader;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import eu.europeana.api.dataset.oaipmh.OAIPMHServiceClient;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
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
     * Generate AuthenticationHandler to access other services via EM ( like keycloak and SR API)
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
     * Creates a authentication handler for SR API access
     * @return authentication for SR API access
     */
    @Bean(SEARCH_RECORD_AUTH_HANDLER)
    public AuthenticationHandler getSearchRecordAccess() {
        return getAuthenticationHandler();
    }

    /**
     * Creates a authentication handler for SR API access
     * @return authentication for SR API access
     */
    @Bean(BEAN_OAI_PMH_CLIENT)
    public OAIPMHServiceClient getOaipmhClient() {
        return new OAIPMHServiceClient();
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

    @Bean(name = SCHEDULED_DATASET_READER)
    @StepScope
    public SynchronizedItemStreamReader<ScheduledDataset> scheduledTaskReader(
            @Value("#{jobParameters[currentStartTime]}") Date currentStartTime) {
        ScheduledDatasetDbReader reader =
                new ScheduledDatasetDbReader(
                        applicationContext.getBean(ScheduleDatasetService.class),
                        settings.getBatchChunkSize(),
                        Filters.eq(ModelConstants.hasBeenProcessed, false),
                        Filters.lte(ModelConstants.created, currentStartTime));
        return threadSafeReader(reader);
    }


    /** Makes ItemReader thread-safe */
    private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
        final SynchronizedItemStreamReader<T> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(reader);
        return synchronizedItemStreamReader;
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

    @Bean(name = DATA_FORMATS_BEAN)
    public Map<RdfFormat, DataFormatter> getFormats() {
        return Map.of(
                RdfFormat.XML, new XMLFormatter(),
                RdfFormat.TURTLE, new TurtleFormatter());
    }
}
