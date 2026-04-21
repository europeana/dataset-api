package eu.europeana.api.dataset.generation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * GeneratorSettings is a Spring configuration class responsible for loading
 * application-specific settings from properties files.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Configuration
@PropertySource("classpath:dataset.generation.properties")
@PropertySource(value = "classpath:dataset.generation.user.properties", ignoreResourceNotFound = true)
public class GeneratorSettings {

    @Value("${search.api.url}")
    private String searchApiUrl;

    @Value("${oaipmh.url}")
    private String oaipmhUrl;

    @Value("${keycloak.token.endpoint}")
    private String keycloakTokenEndpoint;

    @Value("${keycloak.token.grant.params}")
    private String keycloakAccessGrantParams;

    @Value("${dataset.files.location}")
    private String datasetsFolder;

    @Value("${snapshot.file}")
    private String snapshotFile;

    @Value("${slack.webhook}")
    private String slackWebhook;

    @Value("${batch.step.chunkSize: 10}")
    private int batchChunkSize;

    @Value("${batch.step.updates.executor.corePool: 10}")
    private int batchUpdatesCorePoolSize;

    @Value("${batch.step.updates.executor.maxPool: 100}")
    private int batchUpdatesMaxPoolSize;

    @Value("${batch.step.updates.executor.queueSize: 50}")
    private int batchUpdatesQueueSize;

    @Value("${batch.step.updates.throttleLimit: 10}")
    private int batchUpdatesThrottleLimit;

    public String getSearchApiUrl() {
        return searchApiUrl;
    }

    public String getOaipmhUrl() {
        return oaipmhUrl;
    }

    public String getTokenEndpoint() {
        return keycloakTokenEndpoint;
    }

    public String getKeycloakAccessGrantParams() {
        return keycloakAccessGrantParams;
    }

    public String getDatasetsFolder() {
        return datasetsFolder;
    }

    public String getSnapshotFile() {
        return getDatasetsFolder() + snapshotFile;
    }

    public String getLastHarvestDateFile() {
        return getDatasetsFolder() + "last-harvest-date.txt";
    }

    public String getSlackWebhook() {
        return slackWebhook;
    }

    public int getBatchChunkSize() {
        return batchChunkSize;
    }

    public int getBatchUpdatesCorePoolSize() {
        return batchUpdatesCorePoolSize;
    }

    public int getBatchUpdatesMaxPoolSize() {
        return batchUpdatesMaxPoolSize;
    }

    public int getBatchUpdatesQueueSize() {
        return batchUpdatesQueueSize;
    }

    public int getBatchUpdatesThrottleLimit() {
        return batchUpdatesThrottleLimit;
    }
}
