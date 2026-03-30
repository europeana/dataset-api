package eu.europeana.api.dataset.generation.reader.batch;

import dev.morphia.query.filters.Filter;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

import java.util.Iterator;
import java.util.List;

/**
 * A reader class designed to read {@link ScheduledDataset} entities from a database
 * in a paginated manner. This class extends the {@link AbstractPaginatedDataItemReader}
 * to provide support for iteratively fetching batches of data using configurable page sizes.
 *
 * This reader is non-restartable and is designed for multi-threaded usage.
 */
public class ScheduledDatasetDbReader extends AbstractPaginatedDataItemReader<ScheduledDataset> {

    private static final Logger LOG = LogManager.getLogger(ScheduledDatasetDbReader.class);

    private final int readerPageSize;

    private final ScheduleDatasetService scheduleDatasetService;

    private final Filter[] scheduledDatasetFilter;

    public ScheduledDatasetDbReader(ScheduleDatasetService scheduleDatasetService, int pageSize, Filter... scheduledDatasetFilter) {
        this.scheduleDatasetService = scheduleDatasetService;
        this.readerPageSize = pageSize;
        this.scheduledDatasetFilter = scheduledDatasetFilter;
    }

    @Override
    protected Iterator<ScheduledDataset> doPageRead() {
        int start = page * pageSize;

        List<ScheduledDataset> scheduledDatasets =
                scheduleDatasetService.getDatasets(start, pageSize, scheduledDatasetFilter);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieved {} scheduled entities from database. skip={}, limit={}",
                    scheduledDatasets.size(),
                    start,
                    pageSize);

            // TODO remove later
            LOG.debug("DatasetIds={}" + scheduledDatasets.stream().map(ScheduledDataset::getDatasetId).toList());
        }
        return scheduledDatasets.iterator();
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
//        // Non-restartable, as we expect this to run in multi-threaded steps.
//        // see: https://stackoverflow.com/a/20002493
        setSaveState(false);
        setPageSize(readerPageSize);
        setName(getClassName());
    }

    String getClassName() {
        return ScheduledDatasetDbReader.class.getSimpleName();
    }
}
