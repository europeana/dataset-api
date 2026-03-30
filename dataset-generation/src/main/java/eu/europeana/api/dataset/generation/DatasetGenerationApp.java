package eu.europeana.api.dataset.generation;

import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.service.DatasetGenerationExecutor;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_BATCH_SCHEDULED_DATASET_SERVICE;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.api.dataset.generation"}, exclude = {
        SecurityAutoConfiguration.class,    // Remove these exclusions to re-enable security
        DataSourceAutoConfiguration.class  }) // DataSources are manually configured
public class DatasetGenerationApp implements CommandLineRunner {

    private static final Logger LOG = LogManager.getLogger(DatasetGenerationApp.class);

    @Resource(name = BEAN_BATCH_SCHEDULED_DATASET_SERVICE)
    private ScheduleDatasetService scheduleDatasetService;

    @Resource
    private DatasetGenerationExecutor datasetGenerationExecutor;

    @Resource
    SearchApiDatasetReader searchApiDatasetReader;

    @Override
    public void run(String... args) throws Exception {
        LOG.info("Starting Dataset Generation App ...");

        Date lastHarvestDate = scheduleDatasetService.getLatestHarvestDate();
        if(lastHarvestDate == null) {
            LOG.info("No previous harvest date found, All the datasets will be harvested .....");
        }
        List<Dataset> datasetToSchedule = searchApiDatasetReader.getDataset(lastHarvestDate);
        if (!datasetToSchedule.isEmpty()) {
            scheduleDatasetService.scheduleDatasetsForDownload(datasetToSchedule);
        }

        datasetGenerationExecutor.runScheduleDatasets();
    }


    /**
     * Disable web server and run as a stand-alone App for scheduling
     * @param args
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
