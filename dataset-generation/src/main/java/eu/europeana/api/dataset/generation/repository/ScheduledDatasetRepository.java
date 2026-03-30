package eu.europeana.api.dataset.generation.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filter;
import eu.europeana.api.dataset.generation.model.DatasetReport;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.*;


/**
 * This class is responsible for interacting with the database to store and retrieve scheduled datasets.
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Repository
public class  ScheduledDatasetRepository  {

    private final Datastore datastore;

    // Indicates that an update query should be executed as an "upsert",
    // ie. creates new records if they do not already exist, or updates them if they do.
    public static final UpdateOptions UPSERT_OPTS = new UpdateOptions().upsert(true);
    
    @Autowired
    public ScheduledDatasetRepository(
            @Qualifier(BEAN_DATASET_DATA_STORE) Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * Upserts the {@link ScheduledDataset} list to the database
     *
     * @param datasets list of datasets to be scheduled
     * @return BulkWriteResult of db query
     */
    public BulkWriteResult upsertBulk(@NonNull List<ScheduledDataset> datasets) {
        MongoCollection<ScheduledDataset> collection =
                datastore.getCollection(ScheduledDataset.class);

        List<WriteModel<ScheduledDataset>> updates = new ArrayList<>(datasets.size());

        for (ScheduledDataset dataset : datasets) {
            Document updateDoc = new Document(ModelConstants.datasetId, dataset.getDatasetId())
                    .append(ModelConstants.totalSize, dataset.getTotalSize())
                    .append(ModelConstants.hasBeenProcessed, dataset.hasBeenProcessed())
                    .append(ModelConstants.modified, dataset.getModified());

            Document setOnInsertDoc = new Document(ModelConstants.created, dataset.getModified())
                    // manually set Morphia discriminator as we're bypassing its API for this query
                    .append(MORPHIA_DISCRIMINATOR, SCHEDULED_DATASET_CLASSNAME);


            updates.add(new UpdateOneModel<>(new Document(ModelConstants.datasetId, dataset.getDatasetId()),
                    new Document(DOC_SET, updateDoc)
                            // set "created" if this is a new document
                            .append(DOC_SET_ON_INSERT, setOnInsertDoc),
                    UPSERT_OPTS));
        }

        return collection.bulkWrite(updates);
    }

    /**
     * Queries the ScheduledDataset collection to retrieve a page of results
     *
     * @param start - the start index for the results page
     * @param limit - the size of the current results page
     * @param filters Query filters to match the results
     * @return List with results
     */
    public List<ScheduledDataset> getDatasets(int start, int limit, Filter[] filters) {
        return datastore.find(ScheduledDataset.class).filter(filters).iterator(new FindOptions()
                .projection().include(ModelConstants.datasetId, ModelConstants.totalSize).skip(start)
                // matches the index sort order defined in ScheduledDataset
                // sort with _id in case of multiple matching created values
                .sort(descending(ModelConstants.totalSize), ascending(ModelConstants.created)).limit(limit)).toList();

    }

    /**
     * searches the database for scheduled datasets for which the processing is not complete
     * @return The number of scheduled datasets which are not marked as processed
     */
    public long getRuningTasksCount() {
        return datastore.find(ScheduledDataset.class).filter(eq(ModelConstants.hasBeenProcessed, Boolean.FALSE))
                .count();
    }

    /**
     * Marks the given tasks as "processed". Update only occurs if a task matches the specified query
     * filter document.
     *
     * @param datasets list of scheduled datsets
     * @return BulkWriteResult of db query
     */
    public BulkWriteResult markAsProcessed(@NonNull List<ScheduledDataset> datasets) {
        List<WriteModel<ScheduledDataset>> updates = new ArrayList<>(datasets.size());
        for (ScheduledDataset dataset : datasets) {
            updates.add(new UpdateOneModel<>(
                    // query filters on updateType
                    new Document(ModelConstants.datasetId, dataset.getDatasetId()),
                    new Document(DOC_SET, new Document(ModelConstants.hasBeenProcessed, true)
                            .append(ModelConstants.modified, dataset.getModified()))));
        }
        return datastore.getCollection(ScheduledDataset.class).bulkWrite(updates);
    }

    public DeleteResult cleanUpAfterProcessing() {
       return datastore.find(ScheduledDataset.class)
               .filter(eq(ModelConstants.hasBeenProcessed, Boolean.TRUE))
               .delete(new DeleteOptions().multi(true));

    }

    public Date getLastHarvestDate() {
        DatasetReport report = datastore.find(DatasetReport.class).count() > 0
                ? datastore.find(DatasetReport.class).iterator().toList().get(0)
                : new DatasetReport();

        return report.getLastHarvestDate();
    }

    public void updateLastHarvestDate(Date lastHarvestDate) {
        DatasetReport report = datastore.find(DatasetReport.class).count() > 0 ? datastore.find(DatasetReport.class).iterator().toList().get(0) : new DatasetReport();
        report.setLastHarvestDate(lastHarvestDate);
        datastore.save(report);
    }

}
