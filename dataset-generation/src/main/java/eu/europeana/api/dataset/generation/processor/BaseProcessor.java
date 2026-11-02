package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.utils.CheckSumUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.ItemProcessor;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Abstract base class for processing {@link ScheduledDataset} objects.
 *
 * Key Responsibilities:
 * - Serves as an abstraction for dataset processors.
 * - Encapsulates the logic necessary to process {@link ScheduledDataset} objects.
 * @author Srishti singh
 * @since 23 Feb 2026
 */
public abstract class BaseProcessor extends TaskletSupport implements ItemProcessor<ScheduledDataset, ScheduledDataset> {

    abstract ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception;

    abstract ScheduledDataset doRetryProcessing(ScheduledDataset dataset) throws Exception;

    abstract boolean retryNeeded(ScheduledDataset dataset);

    @Override
    public @Nullable ScheduledDataset process(@NonNull ScheduledDataset item) throws Exception {
        ScheduledDataset dataset = doProcessing(item);
        if (retryNeeded(dataset)) {
            return doRetryProcessing(dataset);
        }
        return item;
    }

    /**
     * Performs an atomic swap operation, moving temporary files to their target locations.
     * Each file in the temporary files list is moved to the corresponding location in the target files list.
     * This method attempts to use an atomic move operation, falling back to a regular move if atomic move
     * is not supported by the underlying file system.
     *
     * @param tempFiles  a list of temporary file paths to be moved
     * @param targetFiles  a list of target file paths where the corresponding temporary files should be moved
     */
    protected void performAtomicSwap(List<Path> tempFiles, List<Path> targetFiles) throws IOException {
        for (int i = 0; i < tempFiles.size(); i++) {
            Path temp = tempFiles.get(i);
            Path target = targetFiles.get(i);

            try {
                Files.move(temp, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // fallback if FS doesn't support atomic move
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Generates MD5 checksum files for a list of target files. Each target file will have
     * a corresponding checksum file created in the same directory with its name suffixed by ".md5".
     *
     * @param targetFiles a list of file paths for which MD5 checksum files will be created;
     *                    each path must refer to an existing regular file
     * @throws IOException if any of the specified files do not exist or an error occurs
     *                     during the creation of a checksum file
     */
    protected void createMd5File(List<Path> targetFiles) throws IOException {
        for (Path targetFile : targetFiles) {
            CheckSumUtil.createMd5File(targetFile);
        }
    }


}
