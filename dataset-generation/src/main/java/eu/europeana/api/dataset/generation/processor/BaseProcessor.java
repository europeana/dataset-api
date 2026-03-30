package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.ItemProcessor;

/**
 * Abstract base class for processing {@link ScheduledDataset} objects.
 *
 * Key Responsibilities:
 * - Serves as an abstraction for dataset processors.
 * - Encapsulates the logic necessary to process {@link ScheduledDataset} objects.
 * @author Srishti singh
 * @since 23 Feb 2026
 */
public abstract class BaseProcessor implements ItemProcessor<ScheduledDataset, ScheduledDataset> {

    abstract ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception;

    @Override
    public @Nullable ScheduledDataset process(@NonNull ScheduledDataset item) throws Exception {
        return doProcessing(item);
    }
}
