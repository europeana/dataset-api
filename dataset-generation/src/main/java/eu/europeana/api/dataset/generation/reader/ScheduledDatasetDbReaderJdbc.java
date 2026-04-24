package eu.europeana.api.dataset.generation.reader;

import eu.europeana.api.dataset.generation.exception.DatasetGenerationException;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import jakarta.annotation.Nullable;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

/**
 * A specialized {@link JdbcPagingItemReader} for reading paginated datasets from the `scheduled_dataset` table
 * in the database. This reader retrieves records that have not yet been processed and satisfies specified
 * conditions, ensuring efficient retrieval using pagination.
 *
 * <p>The class is designed to be non-restartable in a multi-threaded job execution environment. It ensures stability
 * and proper sorting of datasets during pagination and retrieval.
 *
 * @author Srishti Singh
 * @since 15 April 2026
 */
public class ScheduledDatasetDbReaderJdbc extends JdbcPagingItemReader<ScheduledDataset> {

    private final int readerPageSize;

    private final DataSource dataSource;

    /**
     * Constructs a new instance of the {@code ScheduledDatasetDbReaderJdbc}.
     *
     * This constructor initializes the JDBC reader with a specified page size, a data source,
     * and a start time for the current job execution. It sets the data source, row mapper,
     * query provider, and parameter values required for executing paginated database queries.
     *
     * @param pageSize the number of records to read per page.
     * @param dataSource the {@link DataSource} to use for connecting to the database.
     * @param currentStartTime the current start time of the job execution used for parameter binding.
     */
    public ScheduledDatasetDbReaderJdbc(int pageSize, DataSource dataSource, Instant currentStartTime) {
        this.readerPageSize = pageSize;
        this.dataSource = dataSource;
    }


    /**
     * Sets the Query Provider of the {@link JdbcPagingItemReader}
     *
     * This method initializes the query provider by invoking the {@code scheduledDatasetQueryProvider} method
     * and sets it as the active query provider for this reader instance.
     *
     * @throws RuntimeException if an error occurs while creating the query provider.
     */
    @Nullable
    public void setQueryProvider() throws DatasetGenerationException {
        try {
            setQueryProvider(scheduledDatasetQueryProvider());
        } catch (Exception e) {
            throw new DatasetGenerationException("Error creating query provider - " +e.getMessage(), e);
        }
    }


    /**
     * Creates and configures a {@link PagingQueryProvider} for retrieving unprocessed datasets from
     * the `scheduled_dataset` table based on specific conditions.
     *
     * The provider uses pagination and ensures datasets are sorted by `total_size` (descending),
     * `created` (ascending), and `dataset_id` (ascending).
     *
     * @return a configured instance of {@link PagingQueryProvider} ready to be used for executing paginated queries.
     * @throws Exception if an error occurs during the creation of the query provider.
     */
    private PagingQueryProvider scheduledDatasetQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(this.dataSource);

        provider.setSelectClause("SELECT dataset_id, total_size, created, modified, has_been_processed");
        provider.setFromClause("FROM scheduled_dataset");
        provider.setWhereClause("WHERE has_been_processed = FALSE AND created <= :currentStartTime");

        provider.setSortKeys(Map.of(
                ModelConstants.total_size, Order.DESCENDING,
                ModelConstants.created, Order.ASCENDING,
                ModelConstants.dataset_id, Order.ASCENDING   // ✅ PRIMARY KEY = safest tie-breaker
        ));

        return provider.getObject();
    }


    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        // Non-restartable, as we expect this to run in multi-threaded steps.
        // see: https://stackoverflow.com/a/20002493
        setSaveState(false);
        setPageSize(readerPageSize);
        setName(getClassName());
    }

    String getClassName() {
        return ScheduledDatasetDbReaderJdbc.class.getSimpleName();
    }
}
