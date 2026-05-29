package eu.europeana.api.dataset.generation.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.europeana.api.dataset.generation.model.DatasetStatus.*;

public class TaskletSupport {

    private static final Logger LOG = LogManager.getLogger(TaskletSupport.class);


    /**
     * Retrieves the last harvest date from the specified file. The file is expected to contain
     * a single ISO-8601 formatted date string. If the file cannot be read or contains invalid
     * data, an error is logged and null is returned.
     *
     * @return the last harvest date as a {@link Date} object if successfully parsed, or null
     *         if an error occurs or the date is invalid.
     */
    public static Date getLastHarvestDate(String lastHarvestFile)  {
        try {
            String content = Files.readString(Path.of(lastHarvestFile)).trim();
            return Date.from(Instant.parse(content));
        } catch (IOException e) {
            LOG.error("Error reading last harvest date file - {}", e.getMessage());
        }
        return null;
    }

    /**
     * Loads a snapshot of dataset identifiers from a file. If the file does not exist,
     * an empty set is returned.
     *
     * @return a set of dataset identifiers loaded from the snapshot file, or an empty set
     * if the file does not exist.
     * @throws IOException if an I/O error occurs while reading the snapshot file.
     */
    public Set<String> loadSnapshot(String snapshotFilePath) {
        Path path = Paths.get(snapshotFilePath);
        if (!Files.exists(path)) {
            return new HashSet<>();
        }
        try (Stream<String> lines = Files.lines(path)) {
            return lines.collect(Collectors.toSet());
        } catch (IOException e) {
            LOG.error("Error reading snapshot file: {}", e.getMessage(), e);
        }
        return new HashSet<>();
    }


    /**
     * Adds information about deleted datasets to a CSV report.
     * Each entry in the `datasetsForRemoval` set is written as a new line in the CSV file,
     * formatted with specific columns indicating the dataset and its status as deleted.
     *
     * @param csvReportPath the path to the CSV file where the information will be appended
     * @param datasetsForRemoval a set of dataset identifiers representing datasets marked for removal
     */
    public static void addDeletedDatasetToReport(String csvReportPath, Set<String> datasetsForRemoval) {
        if (datasetsForRemoval.isEmpty()) {
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvReportPath, true))) {
            for (String dataset : datasetsForRemoval) {
                bw.write(dataset + "," + DELETED + ",-,-");
                bw.newLine();
            }
        } catch (IOException e) {
            LOG.error("Error writing to CSV file: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves the basic file attributes of a given file or directory.
     *
     * @param filePath the path to the file or directory whose attributes are to be read
     * @return the {@link BasicFileAttributes} object containing the file attributes, or null
     *         if an IOException occurs while reading the attributes
     */
    public static BasicFileAttributes getBasicFileAttributes(Path filePath) {
        try {
            return Files.readAttributes(filePath, BasicFileAttributes.class);
        } catch (IOException e) {
            LOG.error("Failed to read file attributes for {}", filePath, e);
            return null;
        }
    }

    /**
     * Counts the occurrences of each status present in the second column of a CSV file.
     *
     * This method reads a CSV file line by line, skipping the header, and extracts the values
     * from the second column of each row. It then aggregates the counts for each unique status.
     *
     * @param csvPath the path to the CSV file to be processed
     * @return a map where the keys are unique status values and the values are their respective counts
     * @throws IOException if an error occurs while reading the CSV file
     */
    public static Map<String, Long> countStatus(Path csvPath) throws IOException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .skip(1)
                    .map(line -> line.split(",")[1])
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));
        }
    }

    /**
     * Builds a formatted table representation from the content of a CSV file.
     * This method reads the provided CSV file, skips the header, and processes the rows
     * up to a specified maximum number. Each row is then formatted into a table-like
     * structure with fixed-width columns for ID, Status, Total, and Failed.
     *
     * @param csvPath the path to the CSV file to be read
     * @param maxRows the maximum number of rows to process from the file
     * @return a string representing the formatted table
     * @throws IOException if an error occurs while reading the CSV file
     */
    public static String buildTable(Path csvPath, int maxRows) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%-15s %-15s %-15s %-15s%n\n",
                "Dataset", "Status", "Total Records", "Failed Records"));

        try (Stream<String> lines = Files.lines(csvPath)) {
            Iterator<String> it = lines.skip(1).iterator();
            int count = 0;

            while (it.hasNext() && count < maxRows) {
                String[] c = it.next().split(",");
                sb.append(String.format("%-15s %-15s %-15s %-15s%n",
                        c[0], c[1], c[2], c[3]));

                count++;
            }
        }
        return sb.toString();
    }
}
