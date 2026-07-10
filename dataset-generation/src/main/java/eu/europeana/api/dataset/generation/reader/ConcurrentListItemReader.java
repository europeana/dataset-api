package eu.europeana.api.dataset.generation.reader;

import org.springframework.batch.item.ItemReader;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe implementation of an {@link ItemReader} that iterates over a list of items
 * in a concurrent manner. This class ensures that each call to {@code read()} retrieves the
 * next item in the list, regardless of concurrent access.
 *
 * @param <ScheduledDataset> The type of the items to read from the list.
 */
public class ConcurrentListItemReader<ScheduledDataset> implements ItemReader<ScheduledDataset> {

    private final List<ScheduledDataset> items;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public ConcurrentListItemReader(List<ScheduledDataset> items) {
        this.items = items;
    }

    @Override
    public ScheduledDataset read() {
        int index = currentIndex.getAndIncrement();
        return index < items.size() ? items.get(index) : null;
    }
}