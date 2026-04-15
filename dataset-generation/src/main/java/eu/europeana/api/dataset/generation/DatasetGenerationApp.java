package eu.europeana.api.dataset.generation;

import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.model.Dataset;
import eu.europeana.api.dataset.generation.reader.SearchApiDatasetReader;
import eu.europeana.api.dataset.generation.service.DatasetGenerationExecutor;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_BATCH_SCHEDULED_DATASET_SERVICE;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.api.dataset.generation"}, exclude = {
        SecurityAutoConfiguration.class,    // Remove these exclusions to re-enable security
})
@EnableJpaRepositories(basePackages =
        "eu.europeana.api.dataset.generation.repository")
@EntityScan(basePackages =
        "eu.europeana.api.dataset.generation")
public class DatasetGenerationApp {
        //implements CommandLineRunner {

    private static final Logger LOG = LogManager.getLogger(DatasetGenerationApp.class);

    @Resource(name = BEAN_BATCH_SCHEDULED_DATASET_SERVICE)
    private ScheduleDatasetService scheduleDatasetService;

    @Resource
    private DatasetGenerationExecutor datasetGenerationExecutor;

    @Resource
    SearchApiDatasetReader searchApiDatasetReader;

    @Resource
    GeneratorSettings settings;

    // Call external service → map response → store into H2 → use in Spring Batch
   // @Override
    @EventListener(ApplicationReadyEvent.class)
    public void start() throws Exception {
        LOG.info("Starting Dataset Generation App ...");

        Date lastHarvestDate = getLastHarvestDate(settings.getLastHarvestDateFile());
        if (lastHarvestDate == null) {
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

    public static Date getLastHarvestDate(String filePath)  {
        try {
            String content = Files.readString(Path.of(filePath)).trim();
            return Date.from(Instant.parse(content));
        } catch (IOException e) {
            LOG.error("Error reading last harvest date file - {}", e.getMessage());
        }
        return null;
    }

}
