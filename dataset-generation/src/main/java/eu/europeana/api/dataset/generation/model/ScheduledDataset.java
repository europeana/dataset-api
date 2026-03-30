package eu.europeana.api.dataset.generation.model;

import dev.morphia.annotations.*;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import org.bson.types.ObjectId;
import java.time.Instant;

/**
 * Represents a dataset that is scheduled for processing or downloading.
 * This class contains metadata about the dataset, including its identifier,
 * size, creation and modification timestamps, and its processing status.
 *
 * An instance of this class is immutable and should be built using its {@link Builder}.
 * Once constructed, the dataset properties cannot be modified directly.
 *
 * The scheduled datasets are designed to be persisted in a database.
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Entity("ScheduledDataset")
@Indexes({
  @Index(
      fields = {@Field(ModelConstants.hasBeenProcessed)}),
      @Index(fields = {@Field(ModelConstants.totalSize), @Field(ModelConstants.created), @Field(ModelConstants.datasetId)}),
})
public class ScheduledDataset {

  @Id private ObjectId dbId;

  @Property(ModelConstants.datasetId)
  private String datasetId;

  @Property(ModelConstants.totalSize)
  private long totalSize;

  /**
   * created not explicitly set. During upserts, we use the value for modified if record doesn't
   * already exist
   */
  @Property(ModelConstants.created)
  private Instant created;

  @Property(ModelConstants.modified)
  private Instant modified;

  @Property(ModelConstants.hasBeenProcessed)
  private boolean hasBeenProcessed;

  @SuppressWarnings("unused")
  private ScheduledDataset() {
    // private constructor
  }

  public ScheduledDataset(
      String datasetId, long totalSize, Instant modified, boolean hasBeenProcessed) {
    this.datasetId = datasetId;
    this.totalSize = totalSize;
    this.modified = modified;
    this.hasBeenProcessed = hasBeenProcessed;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public Instant getCreated() {
    return created;
  }

  public Instant getModified() {
    return modified;
  }

  public boolean hasBeenProcessed() {
    return hasBeenProcessed;
  }

  public static class Builder {
    private final String datasetId;
    private final long totalSize;
    private Instant modified;
    private boolean hasBeenProcessed;

    public Builder(String datasetId, long totalSize) {
      this.datasetId = datasetId;
        this.totalSize = totalSize;
    }

    public Builder modified(Instant modified) {
      this.modified = modified;
      return this;
    }

    public Builder setProcessed(boolean hasBeenProcessed) {
      this.hasBeenProcessed = hasBeenProcessed;
      return this;
    }

    public ScheduledDataset build() {
      return new ScheduledDataset(datasetId, totalSize,  modified, hasBeenProcessed);
    }
  }
}
