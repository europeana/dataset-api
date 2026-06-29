package eu.europeana.api.dataset.generation;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.processor.TaskletSupport;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.service.DatasetGenerationExecutor;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import java.util.*;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_BATCH_SCHEDULED_DATASET_SERVICE;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.api.dataset.generation"}, exclude = {
        SecurityAutoConfiguration.class,    // Remove these exclusions to re-enable security
})
public class DatasetGenerationApp extends TaskletSupport {

    private static final Logger LOG = LogManager.getLogger(DatasetGenerationApp.class);

    @Resource(name = BEAN_BATCH_SCHEDULED_DATASET_SERVICE)
    private ScheduleDatasetService scheduleDatasetService;

    @Resource
    private DatasetGenerationExecutor datasetGenerationExecutor;

    @Resource
    SearchApiDatasetReader searchApiDatasetReader;

    @Resource
    GeneratorSettings settings;

    /**
     * Starts the application workflow for dataset generation. This method is triggered upon the application
     * being fully initialized and ready to process.
     *
     * The workflow consists of the following steps: Call external service → map response → store into H2 → use in Spring Batch
     *
     * 1. Retrieves the last harvest date from a specified file to determine the datasets to be processed.
     *    - If no last harvest date is found, logs a message indicating that all datasets will be harvested.
     *    - If a forced harvest is specified, logs a message indicating that all/ALL datasets will be harvested.
     *    -If a specific set of datasets is specified, logs a message indicating which datasets will be harvested.
     *
     * 2. Reads datasets from the Search API based on the retrieved last harvest date.
     * 3. Maps the response and stores into H2 (in memory DB)
     * 4. Executes the scheduled dataset processing using the dataset generation executor.
     *
     * @throws Exception if any error occurs during the workflow execution.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() throws EuropeanaApiException {
        LOG.info("Starting Dataset Generation App with reader type {} ...", settings.getReaderType());

        Date lastHarvestDate = resolveLastHarvestDate();
        List<Dataset> datasetsToSchedule = fetchDatasets(lastHarvestDate);

        scheduleDatasetService.scheduleDatasetsForDownload(datasetsToSchedule);
        datasetGenerationExecutor.runScheduledDatasets();
    }


    /**
     * Disable web server and run as a stand-alone App for scheduling
     * @param args
     */
    /**
     * The main method serves as the entry point for the application.
     * It initializes the Spring application context, sets up the non-web application mode,
     * and triggers the dataset generation application workflow.
     *
     * @param args the command-line arguments passed to the application. These arguments can be used
     *             to customize the application's behavior or configure specific settings.
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(DatasetGenerationApp.class)
                .web(WebApplicationType.NONE).run(args);

        // ✅ scheduling complete, processing is executed with multiple threads
        if (LOG.isInfoEnabled()) {
            LOG.info("Batch scheduling was completed for {}, " +
                            "waiting for completion of asynchonuous processing ", Arrays.toString(args));
        }

        // 😈 wait for completion of scheduled tasks execution
       // TODO ADD logic to await for the scheduled dataset completion
        context.close();
        System.exit(0);
    }


    /**
     * Resolves the last harvest date to determine the datasets that need to be processed.
     *
     * @return The last harvest date retrieved from the specified file. Returns {@code null}
     *         if a forced harvest is specified or no previous harvest date is found.
     */
    private Date resolveLastHarvestDate() {
        if (settings.isForceHarvest()) {
            LOG.info("Forced harvest of all datasets. datasetsToHarvest set to : {}", settings.getDatasetToHarvest());
            return null;
        }

        Date lastHarvestDate = TaskletSupport.getLastHarvestDate(settings.getLastHarvestDateFile());
        if (lastHarvestDate == null) {
            LOG.info("No previous harvest date found, all datasets will be harvested.");
        }
        return lastHarvestDate;
    }

    /**
     * Fetches datasets based on the provided last harvest date or a specific set of datasets
     * specified in the application settings.
     *
     * If the application settings contain a non-blank list of datasets to harvest, only those
     * datasets will be fetched. Otherwise, datasets modified after the provided last harvest
     * date will be retrieved.
     *
     * @param lastHarvestDate The date used to filter datasets modified after this date. If null,
     *                        all available datasets are considered.
     * @return A list of {@code Dataset} objects representing the datasets retrieved from the
     *         Search API.
     * @throws EuropeanaApiException If an error occurs while interacting with the Search API.
     */
    private List<Dataset> fetchDatasets(Date lastHarvestDate) throws EuropeanaApiException {
        if (StringUtils.isNotBlank(settings.getDatasetToHarvest())) {
            List<String> datasetsToHarvest = new ArrayList<>();
            if (!settings.isForceHarvest()) {
                datasetsToHarvest =
                        Arrays.stream(settings.getDatasetToHarvest().split(","))
                                .map(String::trim)
                                .toList();
                LOG.info("Harvesting specific datasets, datasetsToHarvest set to : {}", datasetsToHarvest);
            }
            return searchApiDatasetReader.getDataset(null, datasetsToHarvest);
        }

        return searchApiDatasetReader.getDataset(lastHarvestDate);
    }
}
