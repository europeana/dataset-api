package eu.europeana.api.dataset.generation.repository;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * This class is responsible for interacting with the database to store and retrieve scheduled datasets.
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Repository
public interface ScheduledDatasetRepository extends JpaRepository<ScheduledDataset, String> {

    List<ScheduledDataset> findAllById(Iterable<String> ids);

    List<ScheduledDataset> findByHasBeenProcessedFalse();
}
