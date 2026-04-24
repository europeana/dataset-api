package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.exception.DatasetGenerationException;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.utils.ProgressLogger;
import eu.europeana.api.dataset.oaipmh.OAIPMHServiceClient;
import eu.europeana.api.dataset.oaipmh.exception.OaiPmhClientException;
import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.model.Record;
import eu.europeana.api.dataset.oaipmh.utils.OAIPMHQueryUtils;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_OAI_PMH_CLIENT;
import static eu.europeana.api.dataset.oaipmh.utils.OAIPMHQueryUtils.LIST_RECORDS_VERB;

/**
 * Service responsible for streaming and processing records obtained from an OAI-PMH
 * (Open Archives Initiative Protocol for Metadata Harvesting) compliant endpoint.
 * This service interacts with the specified dataset and leverages a RecordSink to consume the records.
 */
@Service
public class ListRecordService {

    private static final Logger LOG = LogManager.getLogger(ListRecordService.class);
    private static final Long INITIAL_DELAY_MS = 1000L;

    @Resource
    GeneratorSettings settings;

    @Resource(name = BEAN_OAI_PMH_CLIENT)
    OAIPMHServiceClient client;

    /**
     * Streams records from a specified dataset and processes them using a provided RecordSink.
     * This method fetches records in pages from an OAI-PMH compliant endpoint, processes
     * them individually, and logs the progress periodically.
     *
     * @param dataset the dataset containing the record set to be streamed, including metadata such as dataset ID and total size
     * @param sink the sink used to consume and process each streamed record
     * @throws Exception if an error occurs during the record streaming or processing
     * @return failed records count
     */
    @SuppressWarnings("java:S109")
    public long streamRecords(ScheduledDataset dataset, RecordSink sink) throws EuropeanaApiException, IOException {
        LOG.info("Streaming records for set {}", dataset.getDatasetId());
        ProgressLogger logger = new ProgressLogger(dataset.getDatasetId(), dataset.getTotalSize(), 30);

        long counter = 0;
        long failedRecords = 0;
        long start = System.currentTimeMillis();
        String resumptionToken = null;
        try {
            do {
                String request = OAIPMHQueryUtils.buildListRecordQuery(
                        settings.getOaipmhUrl(),
                        LIST_RECORDS_VERB,
                        "edm",
                        dataset.getDatasetId(),
                        resumptionToken
                );

                //OaiPage response = client.executeAndGetResponse(request);
                OaiPage response = executeWithRetry(request);
                if (response == null) {
                    break;
                }

                List<Record> records = response.getRecords();
                if (records != null && !records.isEmpty()) {
                    for (Record recordToConsume : records) {
                        sink.consume(recordToConsume);
                        counter++;
                        logger.logProgress(counter);
                    }
                }

                resumptionToken = (response.getResumptionToken() != null)
                        ? response.getResumptionToken()
                        : null;

            } while (resumptionToken != null && !resumptionToken.isEmpty());

        } catch (URISyntaxException e) {
            throw new DatasetGenerationException("Error creating the ListRecordQuery url - "+e.getMessage(), e);
        } finally {
            failedRecords = dataset.getTotalSize() - counter;
            LOG.info(
                    "Dataset: {} Total records: {}, Downloaded: {}, Failed records: {}",
                    dataset.getDatasetId(),
                    dataset.getTotalSize(),
                    counter,
                    dataset.getTotalSize() - counter
            );
            sink.close();
        }
        LOG.info(
                "ListRecords for set {} executed in {} ms. Harvested {} records.",
                dataset.getDatasetId(),
                (System.currentTimeMillis() - start),
                counter
        );
        return failedRecords;
    }

    /**
     * Executes a given OAI-PMH request with retry logic in case of transient failures.
     * The method will attempt to execute the request up to the specified number of retries,
     * with an exponential backoff delay between attempts. If all attempts fail, an exception is thrown.
     *
     * Retries only for the OaiPmhClientException (which is thrown if the http status is not 200 OK)
     * See {@link OAIPMHServiceClient#executeAndGetResponse(String)}
     *
     * Retries only the unstable part (API call) which avoids duplicating already processed records
     * Uses exponential backoff (1s → 2s → 4s)
     * INITIAL_DELAY_MS - the initial delay in milliseconds before the first retry attempt,
     *        which doubles after each subsequent failure
     *
     * @param request the OAI-PMH request to be executed
     * @return the response as an {@link OaiPage} object if the execution is successful
     * @throws EuropeanaApiException if all retry attempts fail or an unexpected error occurs during request execution
     */
    private OaiPage executeWithRetry(String request) throws EuropeanaApiException {
        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (true) {
            try {
                return client.executeAndGetResponse(request);
            } catch (OaiPmhClientException e) {
                attempt++;

                if (attempt > settings.getMaxRetries()) {
                    LOG.error("Max retries reached for request: {}", request, e);
                    throw new EuropeanaApiException(e.getMessage(), e);
                }

                LOG.warn("Attempt {} failed. Retrying in {} ms...", attempt, delay, e);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                delay *= 2; // exponential backoff
            }
        }
    }
}
