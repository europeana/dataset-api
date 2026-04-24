package eu.europeana.api.dataset.generation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;

/**
 * The ProgressLogger class is responsible for logging progress updates while processing a set of items.
 * It logs information such as the number of items processed, processing rate, and estimated time remaining.
 *
 * @author Srishti Singh
 * @since 09 March 2026
 */
public class ProgressLogger {

    private static final Logger LOG = LogManager.getLogger(ProgressLogger.class);

    private final String setName;
    private final long startTime;
    private long totalItems;
    private final int logAfterSeconds;

    private long lastLogTime;

    public ProgressLogger(String setName, long totalItems, int logAfterSeconds) {
        this.setName = setName;
        this.totalItems = totalItems;
        this.logAfterSeconds = logAfterSeconds;
        this.startTime = System.currentTimeMillis();
        this.lastLogTime = this.startTime;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public void logProgress(long itemsDone) {
        long now = System.currentTimeMillis();

        if (!shouldLog(now)) {
            return;
        }

        if (totalItems <= 0) {
            LOG.info("Set {} - Retrieved {} items", setName, itemsDone);
            lastLogTime = now;
            return;
        }

        long elapsedMs = now - startTime;

        if (elapsedMs <= 0 || itemsDone <= 0) {
            return; // avoid division issues
        }

        double itemsPerSecond = (itemsDone * 1000.0) / elapsedMs;
        long remainingItems = Math.max(0, totalItems - itemsDone);

        long remainingMs = (long) (remainingItems / (itemsPerSecond / 1000.0));

        logWithRate(itemsDone, itemsPerSecond, remainingMs);

        lastLogTime = now;
    }

    private boolean shouldLog(long now) {
        if (logAfterSeconds <= 0) {
            return true;
        }
        long elapsedSinceLastLog = now - lastLogTime;
        return elapsedSinceLastLog >= logAfterSeconds * 1000L;
    }

    private void logWithRate(long itemsDone, double itemsPerSecond, long remainingMs) {
        if (itemsPerSecond >= 1.5) {
            LOG.info(
                    "Set {} - Retrieved {} of {} ({} records/sec). Time remaining: {}",
                    setName,
                    itemsDone,
                    totalItems,
                    Math.round(itemsPerSecond),
                    formatDuration(remainingMs)
            );
        } else {
            long perMinute = Math.round(itemsPerSecond * 60);
            LOG.info(
                    "Set {} - Retrieved {} of {} ({} records/min). Time remaining: {}",
                    setName,
                    itemsDone,
                    totalItems,
                    perMinute,
                    formatDuration(remainingMs)
            );
        }
    }

    public static String formatDuration(long durationMs) {
        Duration duration = Duration.ofMillis(durationMs);

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}