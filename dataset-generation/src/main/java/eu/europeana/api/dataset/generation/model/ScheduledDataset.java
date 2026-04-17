package eu.europeana.api.dataset.generation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Represents a scheduled dataset with metadata information.
 * This class corresponds to a database entity mapped to the "scheduled_dataset" table.
 * Annotations:
 * - {@code @Entity}: Indicates that this class is a JPA entity.
 * - {@code @Table(name = "scheduled_dataset")}: Maps the entity to the "scheduled_dataset" table in a database.
 * - {@code @Id}: Marks the datasetId field as the primary key of the entity.
 *
 * @author Srishti Singh
 * @since 15 April 2026
 */
@Entity
@Table(name = "scheduled_dataset")
public class ScheduledDataset {

    @Id
    private String datasetId; // ✅ PRIMARY KEY now

    private long totalSize;
    private Instant created;
    private Instant modified;
    private boolean hasBeenProcessed;

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public void setHasBeenProcessed(boolean hasBeenProcessed) {
        this.hasBeenProcessed = hasBeenProcessed;
    }
}
