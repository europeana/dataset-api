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

    /**
     * The core pool size says a thread pool executor will start with N number of threads.
     */
    @Value("${batch.step.updates.executor.corePool: 10}")
    private int batchUpdatesCorePoolSize;

    /**
     * A throttle-limit T says that, regardless of the number of threads available
     * in the thread pool (batchUpdatesCorePoolSize), only use T of those threads for a tasklet.
     */
    @Value("${batch.step.updates.throttleLimit: 10}")
    private int batchUpdatesThrottleLimit;

    /**
     * If all (batchUpdatesCorePoolSize) threads are busy and new task comes up,
     * then It will keep tasks in queue.
     */
    @Value("${batch.step.updates.executor.queueSize: 30}")
    private int batchUpdatesQueueSize;

    /*
    * If queue is full it will create 11th thread and will go till maxPool value.
     */
    @Value("${batch.step.updates.executor.maxPool: 25}")
    private int batchUpdatesMaxPoolSize;

    /**
     * Progress logger logging interval in seconds
     * By default 2 minutes (120 seconds)
     */
    @Value("${log.interval: 120}")
    private int logInterval;

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

    public String getFailedSetsFile() {
        return getDatasetsFolder() + "failed-sets.txt";
    }

    /**
     * Determines if the harvest should be forced for all datasets.
     * Checks whether the dataset to harvest is set to "ALL", ignoring case.
     *
     * @return true if the dataset to harvest is "ALL", false otherwise.
     */
    public boolean isForceHarvest() {
        return StringUtils.isNotEmpty(getDatasetToHarvest()) && StringUtils.equalsIgnoreCase(getDatasetToHarvest(),  "ALL");
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

    public int getLogInterval() {
        return logInterval;
    }
}
