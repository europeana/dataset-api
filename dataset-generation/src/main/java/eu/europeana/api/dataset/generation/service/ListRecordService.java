package eu.europeana.api.dataset.generation.service;

import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.utils.ProgressLogger;
import eu.europeana.api.dataset.oaipmh.OAIPMHServiceClient;
import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.model.Record;
import eu.europeana.api.dataset.oaipmh.utils.OAIPMHQueryUtils;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_OAI_PMH_CLIENT;
import static eu.europeana.api.dataset.oaipmh.utils.OAIPMHQueryUtils.LIST_RECORDS_VERB;

@Service
public class ListRecordService {

    private static final Logger LOG = LogManager.getLogger(ListRecordService.class);

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
     */
    public void streamRecords(ScheduledDataset dataset, RecordSink sink) throws Exception {
        LOG.info("Streaming records for set {}", dataset.getDatasetId());
        ProgressLogger logger = new ProgressLogger(dataset.getDatasetId(), dataset.getTotalSize(), 30);

        long counter = 0;
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

                OaiPage response = client.executeAndGetResponse(request);

                if (response == null) {
                    break;
                }

                List<Record> records = response.getRecords();
                if (records != null && !records.isEmpty()) {

                    for (Record record : records) {
                        sink.consume(record);
                        counter++;

                        logger.logProgress(counter);
                    }
                }


                resumptionToken = (response.getResumptionToken() != null)
                        ? response.getResumptionToken()
                        : null;

            } while (resumptionToken != null && !resumptionToken.isEmpty());

        } finally {
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
    }
}
