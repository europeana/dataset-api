package eu.europeana.api.dataset.generation.writer;

import eu.europeana.api.dataset.generation.listener.DatasetReportListener;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;

import org.jspecify.annotations.NonNull;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;

/**
 * ScheduledDatasetWriter is a specialized implementation of {@code ItemWriter} and {@code ItemStream}
 * that delegates dataset writing operations to a {@code FlatFileItemWriter} while also integrating
 * reporting and status update functionalities for {@code ScheduledDataset} objects.
 *
 * Responsibilities:
 * - Handles the status processing and report accumulation logic for {@code ScheduledDataset} items.
 * - Delegates stream operations to the underlying {@code FlatFileItemWriter}.
 *
 */
public class ScheduledDatasetWriter implements ItemWriter<ScheduledDataset>, ItemStream {

    private final FlatFileItemWriter<ScheduledDataset> delegate;

    private final DatasetReportListener datasetReportListener;

    /**
     * Constructs a new instance of ScheduledDatasetWriter with the specified delegate writer
     * and dataset report listener.
     *
     * @param delegate the FlatFileItemWriter to which the dataset writing operations are delegated.
     * @param datasetReportListener the listener used to accumulate dataset report information
     *                              and update the dataset's status.
     */
    public ScheduledDatasetWriter(FlatFileItemWriter<ScheduledDataset> delegate,
                                  DatasetReportListener datasetReportListener) {
        this.delegate = delegate;
        this.datasetReportListener = datasetReportListener;
    }


    @Override
    public void write(@NonNull Chunk<? extends ScheduledDataset> chunk) throws Exception {
        for (ScheduledDataset dataset : chunk) {
           dataset.setStatus( datasetReportListener.accumulate(dataset));
        }
        delegate.write(chunk);
    }

    @Override
    public void open(ExecutionContext executionContext) {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        delegate.update(executionContext);
    }

    @Override
    public void close() {
        delegate.close();
    }

}
