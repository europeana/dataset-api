package eu.europeana.api.dataset.generation.deletion;

import java.io.IOException;
import java.util.Set;

/**
 * The DeletionService interface provides a contract for implementing file
 * deletion services. Implementations of this interface are responsible for
 * removing files associated with specific dataset IDs.
 *
 * Implementations should consider factors such as file extensions, storage
 * directories, and logging of operations.
 */
public interface DeletionService {

    /**
     * Deletes files associated with the provided dataset identifiers.
     * The method removes files mapped to each dataset ID in the input set.
     *
     * @param datasetsForRemoval a set of dataset identifiers whose associated files need to be deleted.
     * @throws IOException if an I/O error occurs during the deletion process.
     */
    void deleteFiles(Set<String> datasetsForRemoval) throws IOException;
}
