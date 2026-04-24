package eu.europeana.api.dataset.generation.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * GeneratorSettings is a Spring configuration class responsible for loading
 * application-specific settings from properties files.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Configuration
@PropertySource(
        value = {"classpath:dataset.generation.properties", "classpath:dataset.generation.user.properties"},
        ignoreResourceNotFound = true)
public class GeneratorSettings {

    @Value("${search.api.url}")
    private String searchApiUrl;

    @Value("${oaipmh.url}")
    private String oaipmhUrl;

    @Value("${keycloak.token.endpoint}")
    private String keycloakTokenEndpoint;

    @Value("${keycloak.token.grant.params}")
    private String keycloakAccessGrantParams;

    @Value("${dataset.to.harvest}")
    private String datasetToHarvest;

    @Value("${max.retry:2}")
    private int maxRetries;

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

    public String getDatasetToHarvest() {
        return datasetToHarvest;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getDatasetsFolder() {
        return datasetsFolder;
    }

    public String getSnapshotFile() {
        return getDatasetsFolder() + snapshotFile;
    }

    public String getCsvReportPath() {
        return getDatasetsFolder() +
                "status_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) +
                ".csv";
    }

    public String getFailedSetsFile() {
        return getDatasetsFolder() + "failed-sets.txt";
    }

    public boolean isForceHarvest() {
        return StringUtils.equalsIgnoreCase(getDatasetToHarvest(),  "ALL");
    }

    public String getLastHarvestDateFile() {
        return getDatasetsFolder() + "lastHarvestDate.txt";
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
