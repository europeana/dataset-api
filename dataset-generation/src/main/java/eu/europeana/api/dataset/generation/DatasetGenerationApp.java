package eu.europeana.api.dataset.generation;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.processor.TaskletSupport;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.service.DatasetGenerationExecutor;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
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
     * 2. Reads datasets from the Search API based on the retrieved last harvest date.
     * 3. Maps the response and stores into H2 (in memory DB)
     * 4. Executes the scheduled dataset processing using the dataset generation executor.
     *
     * @throws Exception if any error occurs during the workflow execution.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() throws EuropeanaApiException {
        LOG.info("Starting Dataset Generation App ...");

        Date lastHarvestDate = TaskletSupport.getLastHarvestDate(settings.getLastHarvestDateFile());

        if (lastHarvestDate == null) {
            LOG.info("No previous harvest date found, All the datasets will be harvested .....");
        }

        if (settings.isForceHarvest()) {
            lastHarvestDate = null;
            LOG.info("Forced harvest of all the datasets. datasetsToHarvest: {}", settings.getDatasetToHarvest() );
        }


        // List<Dataset> datasetToSchedule = searchApiDatasetReader.getDataset(lastHarvestDate);


        List<Dataset> datasetToSchedule = new ArrayList<>();
        //  datasetToSchedule.add(new Dataset("1536",1623 ));
        datasetToSchedule.add(new Dataset("1533",1 ));
        datasetToSchedule.add(new Dataset("536",392 ));
        datasetToSchedule.add(new Dataset("1514",4 ));
        datasetToSchedule.add(new Dataset("1524",1 ));

        scheduleDatasetService.scheduleDatasetsForDownload(datasetToSchedule);
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
//
        context.close();
        System.exit(0);
    }
}
