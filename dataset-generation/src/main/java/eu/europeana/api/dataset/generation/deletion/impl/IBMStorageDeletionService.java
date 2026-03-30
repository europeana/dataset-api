package eu.europeana.api.dataset.generation.deletion.impl;

import eu.europeana.api.dataset.generation.deletion.DeletionService;

import java.io.IOException;
import java.util.Set;

public record IBMStorageDeletionService(String storagePath) implements DeletionService {

    @Override
    public void deleteFiles(Set<String> datasetsForRemoval) throws IOException {
        // for future
    }
}
