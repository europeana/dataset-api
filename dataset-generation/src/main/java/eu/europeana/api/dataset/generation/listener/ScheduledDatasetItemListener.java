package eu.europeana.api.dataset.generation.listener;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.listener.ItemListenerSupport;

/**
 * Listener class for handling events related to items of type {@link ScheduledDataset}.
 * Extends {@link ItemListenerSupport} and specifically listens to read and write
 * operations involving {@link ScheduledDataset}.
 *
 * This listener primarily interacts with the {@link ScheduleDatasetService} to perform
 * post-processing on scheduled dataset items.
 */
public class ScheduledDatasetItemListener extends ItemListenerSupport<ScheduledDataset, ScheduledDataset> {

    private static final Logger LOG = LogManager.getLogger(ScheduledDatasetItemListener.class);

    private final ScheduleDatasetService scheduleDatasetService;

    /**
     * Constructor for {@link ScheduledDatasetItemListener}.
     * @param scheduleDatasetService
     */
    public ScheduledDatasetItemListener(ScheduleDatasetService scheduleDatasetService) {
        this.scheduleDatasetService = scheduleDatasetService;
    }

    @Override
    public void afterRead(ScheduledDataset item) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Item read: {} with totalSize : {}", item.getDatasetId(), item.getTotalSize());
        }
    }
}
