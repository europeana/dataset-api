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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    SlackConnection slackConnection;

    @Resource
    GeneratorSettings settings;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
       updateLastHarvestDate();
       slackConnection.publishStatusReport(buildSlackMessage(Path.of(settings.getCsvReportPath())).toString());
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
     * Builds a Slack message containing a report based on the contents of a CSV file.
     * The message includes a dataset report header, a summary of status counts,
     * and a preview of a table containing dataset information.
     *
     * @param csvPath the path to the CSV file containing dataset information
     * @return a map representing the message structure in Slack block format
     * @throws IOException if an error occurs while reading the CSV file
     */
    public Map<String, Object> buildSlackMessage(Path csvPath) throws IOException {
        Map<String, Long> counts = countStatus(csvPath);
        String table = buildTable(csvPath, 10); // max 10 rows for now, a full report will be attached

        String summary = counts.entrySet().stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n"));

        List<Map<String, Object>> blocks = new ArrayList<>();

        // Header
        blocks.add(section("📊 *Dataset Report*"));

        // Summary block
        blocks.add(section("```" + summary + "```"));

        // Table preview
        blocks.add(section("```" + table + "```"));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Slack message blocks: {}", blocks);
        }

        return Map.of("blocks", blocks);
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
