package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.slack.SlackConnection;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The DatasetReportTasklet class is a Spring Batch Tasklet implementation that performs two main tasks:
 * - Updates the last harvest date to a file in the specified location.
 * - Sends a message to a configured Slack channel indicating the successful completion of dataset generation.
 *
 * Key behaviors:
 * - Formats timestamps using the "yyyy-MM-dd'T'HH:mm:ss'Z'" format and the system's default time zone.
 * - Logs information and errors using the Apache Log4j logging framework.
 */
@Service
public class DatasetReportTasklet implements Tasklet {

    private static final Logger LOG = LogManager.getLogger(DatasetReportTasklet.class);

    public static final String HARVEST_DATE_FORMAT      = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Resource
    SlackConnection slackConnection;

    @Resource
    GeneratorSettings settings;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
       updateLastHarvestDate();
       slackConnection.publishStatusReport("datasets generation completed successfully");

       return RepeatStatus.FINISHED;
    }

    /**
     * Updates the file containing the last harvest date with the current timestamp.
     * - Formats the current timestamp using the "yyyy-MM-dd'T'HH:mm:ss'Z'" pattern and system's default time zone.
     */
    public void updateLastHarvestDate() {
        try {
            Path filePath = Path.of(settings.getLastHarvestDateFile());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HARVEST_DATE_FORMAT)
                    .withZone(ZoneId.systemDefault());
            String formattedDate = formatter.format(Instant.now());
            Files.writeString(filePath, formattedDate);
            LOG.info("Last harvest date updated to {}", formattedDate);
        } catch (IOException e) {
            LOG.error("Error writing the {} file ", settings.getLastHarvestDateFile(), e);
        }
    }
}
