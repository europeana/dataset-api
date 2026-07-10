package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.model.ReaderType;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.LIST_SCHEDULED_DATASET_READER;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.READER_FACTORY_BEAN;

/**
 * Factory class responsible for providing the appropriate {@link ItemReader} implementation
 * based on the specified {@link ReaderType}.
 */
@Component(READER_FACTORY_BEAN)
public class ReaderFactory {

    /**
     * A {@link JdbcPagingItemReader} implementation that performs paginated reads
     * from a database to retrieve {@link ScheduledDataset} entities.
     * The reader supports pagination and ensures efficient fetching of records by
     * leveraging database cursors or similar mechanisms.
     * NOT threadsafe
     */
    private final JdbcPagingItemReader<ScheduledDataset> jdbcReader;

    /**
     * A thread-safe item reader implementation for reading {@link ScheduledDataset} entities.
     *
     * This {@code SynchronizedItemReader} provides serialized access to data sources,
     * ensuring thread safety when used in multi-threaded or concurrent batch processing contexts.
     */
    private final ItemReader<ScheduledDataset> listReader;

    public ReaderFactory(
            JdbcPagingItemReader<ScheduledDataset> jdbcReader,
            @Qualifier(LIST_SCHEDULED_DATASET_READER) ItemReader<ScheduledDataset> listReader) {
        this.jdbcReader = jdbcReader;
        this.listReader = listReader;
    }

    public ItemReader<ScheduledDataset> getReader(ReaderType type) {
        return switch (type) {
            case JDBC -> jdbcReader;
            case LIST -> listReader;
        };
    }
}