package eu.europeana.api.dataset.generation.reader;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;

/**
 * A reader class designed to read {@link ScheduledDataset} entities from a database
 * in a paginated manner. This class extends the {@link AbstractPaginatedDataItemReader}
 * to provide support for iteratively fetching batches of data using configurable page sizes.
 *
 * This reader is non-restartable and is designed for multi-threaded usage.
 */
public class ScheduledDatasetDbReader extends JpaPagingItemReader<ScheduledDataset> {

    private static final Logger LOG = LogManager.getLogger(ScheduledDatasetDbReader.class);

    private final int readerPageSize;

    private final EntityManagerFactory entityManagerFactory;

    private final String queryString;
    public ScheduledDatasetDbReader(int pageSize, EntityManagerFactory entityManagerFactory, String queryString) {
        this.readerPageSize = pageSize;
        this.entityManagerFactory = entityManagerFactory;
        this.queryString = queryString;

        setEntityManagerFactory(this.entityManagerFactory);
        setQueryString(this.queryString);
        setSaveState(false);
        setPageSize(readerPageSize);
        setName(getClassName());

    }


//    @Override
//    protected void doOpen() throws Exception {
//        super.doOpen();
////        // Non-restartable, as we expect this to run in multi-threaded steps.
////        // see: https://stackoverflow.com/a/20002493
//        setSaveState(false);
//        setPageSize(readerPageSize);
//        setName(getClassName());
//    }

    String getClassName() {
        return ScheduledDatasetDbReader.class.getSimpleName();
    }
}
