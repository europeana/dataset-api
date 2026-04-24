package eu.europeana.api.dataset.generation.deletion.impl;

import eu.europeana.api.dataset.generation.deletion.DeletionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

/**
 * FileDeletionService is an implementation of the DeletionService interface
 * that provides functionality to delete files from a specified storage path.
 * It identifies files for deletion based on the provided dataset IDs and only
 * handles files with a ".zip" extension.
 *
 * As storagePath is Immutable, hence record class
 *
 * @author Srishti Singh
 * @since 23 March 2026
 */
public record FileDeletionService(String storagePath) implements DeletionService {

    private static final Logger LOG = LogManager.getLogger(FileDeletionService.class);

    @SuppressWarnings("java:S109")
    @Override
    public void deleteFiles(Set<String> datasetsForRemoval) throws IOException {
        if (datasetsForRemoval.isEmpty()) {
            LOG.info("No files to delete");
            return;
        }

        Path dir = Paths.get(this.storagePath);
        if (!Files.exists(dir)) {
            LOG.warn("Zip directory does not exist: {}", this.storagePath);
            return;
        }

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.toString().endsWith(".zip"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String id = fileName.substring(0, fileName.length() - 4); // remove ".zip"

                        if (datasetsForRemoval.contains(id)) {
                            try {
                                Files.delete(path);
                                LOG.info("Deleted file: {}", path);
                            } catch (IOException e) {
                                LOG.error("Failed to delete file: {}", path, e);
                            }
                        }
                    });
        }
    }
}
