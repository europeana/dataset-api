package eu.europeana.api.dataset.generation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons_sb3.slack.SlackConnection;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.STATUS_REPORT_CSV_PATH_BEAN;

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
public class JobCompletionTasklet extends TaskletSupport implements Tasklet {

    private static final Logger LOG = LogManager.getLogger(JobCompletionTasklet.class);

    public static final String HARVEST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    SlackConnection slackConnection;

    @Resource
    GeneratorSettings settings;

    @Resource
    ScheduleDatasetService scheduleDatasetService;

    @Resource(name = STATUS_REPORT_CSV_PATH_BEAN)
    String statusReportCsvFile;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
       updateLastHarvestDate();
       updateFailedSetsFile();
       slackConnection.publishStatusReport(buildSlackMessage());
       LOG.info("Job completed successfully...!!");
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

    /**
     * Updates the file containing the list of datasets that have failed processing.
     *
     * This method retrieves a list of dataset IDs that have not been successfully processed
     * from the schedule dataset service. The IDs are written to a file specified in the application
     * settings. If the list is empty, an informational log is recorded, and no file operations are performed.
     *
     * Logging:
     * - Logs an informational message if no failed datasets are found.
     * - Logs an error message if an IOException occurs during file operations.
     * - Logs an informational message on the successful update of the failed datasets file.
     *
     * Behavior:
     * - Fetches dataset IDs that are marked as 'not processed' by the ScheduleDatasetService.
     * - Writes the dataset IDs to a file defined by the application settings.
     * - Ensures proper resource management by using a try-with-resources block for file writing.
     */
    public void updateFailedSetsFile() {
        List<String> dataset = scheduleDatasetService.findHasBeenProcessedFalse()
                .stream().map(
                        d -> d.getDatasetId())
                .collect(Collectors.toList());

        if (dataset.isEmpty()) {
            LOG.info("No failed datasets found");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(settings.getFailedSetsFile()))) {
            for(String ds : dataset) {
                writer.write(ds);
            }
        } catch (IOException e) {
           LOG.error("Error writing the {} file ", settings.getFailedSetsFile(), e);
        } finally {
            LOG.info("Failed sets file updated with datasets {}" , dataset);
        }
    }

    /**
     * Builds a Slack message containing a report based on the contents of a CSV file.
     * The message includes a dataset report header, a summary of status counts,
     * and a preview of a table containing dataset information.
     *
     * @return a map representing the message structure in Slack block format
     * @throws IOException if an error occurs while reading the CSV file
     */
    public String buildSlackMessage() throws IOException {
        Path csvPath = Path.of(statusReportCsvFile);
        Map<String, Long> counts = countStatus(csvPath);
        String table = buildTable(csvPath, 10);// max 10 rows for now, a full report will be attached
        long total = scheduleDatasetService.count();

        String summary = counts.entrySet().stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n"));

        List<Map<String, Object>> blocks = new ArrayList<>();

        blocks.add(section("📊 *Dataset Report*"));  // Header
        blocks.add(section(total + " datasets were processed, see full report <" + getFullReportLink(csvPath) + "|here>"));
        blocks.add(section("```" + summary + "```")); // Summary block
        blocks.add(section("```" + table + "```"));  // Table preview

        String jsonPayload = objectMapper.writeValueAsString(Map.of("blocks", blocks));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Slack message blocks: {}", jsonPayload);
        }
        return jsonPayload;
    }

    private String getFullReportLink(Path csvPath) {
        if (StringUtils.endsWith(settings.getDatasetServingUrl(), "/")) {
            return settings.getDatasetServingUrl() + csvPath.getFileName();
        }
        return settings.getDatasetServingUrl() + "/" + csvPath.getFileName();
    }

    /**
     * Constructs a Slack message section containing text formatted as Markdown.
     * Helper method for building blocks in the Slack message structure.
     *
     * @param text the text to include in the section; should be compatible with Slack's Markdown formatting
     * @return a map representing the section block in Slack's block kit structure
     */
    private Map<String, Object> section(String text) {
        return Map.of(
                "type", "section",
                "text", Map.of(
                        "type", "mrkdwn",
                        "text", text
                )
        );
    }

}
