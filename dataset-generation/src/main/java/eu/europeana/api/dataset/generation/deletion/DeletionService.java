package eu.europeana.api.dataset.generation.deletion;

import java.io.IOException;
import java.util.Set;

public interface DeletionService {

    void deleteFiles(Set<String> datasetsForRemoval) throws IOException;
}
