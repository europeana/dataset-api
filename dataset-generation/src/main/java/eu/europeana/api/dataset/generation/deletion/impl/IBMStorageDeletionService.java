package eu.europeana.api.dataset.generation.deletion.impl;

import eu.europeana.api.dataset.generation.deletion.DeletionService;

import java.io.IOException;
import java.util.Set;

/**
 * IBM Storage Deletion Service
 * @param storagePath storage path
 */
public record IBMStorageDeletionService(String storagePath) implements DeletionService {

    /**
     * Deletes files associated with the provided dataset identifiers.
     * @param datasetsForRemoval a set of dataset identifiers whose associated files need to be deleted.
     * @throws IOException expection during deletion
     */
    @Override
    public void deleteFiles(Set<String> datasetsForRemoval) throws IOException {
        // for future
    }
}
