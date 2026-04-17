package eu.europeana.api.dataset.generation.listener;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.Chunk;
import java.util.List;

/**
 * Listener class for handling events related to items of type {@link ScheduledDataset}.
 * Extends {@link ItemListenerSupport} and specifically listens to read and write
 * operations involving {@link ScheduledDataset}.
 *
 * This listener primarily interacts with the {@link ScheduleDatasetService} to perform
 * post-processing on scheduled dataset items.
 */
public class ScheduledDatasetItemListener extends ItemListenerSupport<ScheduledDataset, ScheduledDataset> {

    private final ScheduleDatasetService scheduleDatasetService;

    /**
     * Constructor for {@link ScheduledDatasetItemListener}.
     * @param scheduleDatasetService
     */
    public ScheduledDatasetItemListener(ScheduleDatasetService scheduleDatasetService) {
        this.scheduleDatasetService = scheduleDatasetService;
    }

    @Override
    public void afterWrite(Chunk<? extends ScheduledDataset> items) {
        scheduleDatasetService.markDatasetAsProcessed((List<ScheduledDataset>) items.getItems());
    }
}
