package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ListRecordService;

/**
 * A processor for harvesting OAI-PMH records from a given dataset and streaming
 * them to IBM systems.
 */
public class OaiPmhIBMProcessor extends BaseProcessor {

    private final ListRecordService listRecordService;

    public OaiPmhIBMProcessor(ListRecordService listRecordService) {
        this.listRecordService = listRecordService;
    }

    @Override
    ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception {
//        try (IBMRecordSink ibmSink = new IBMRecordSink()) {
//            listRecordService.streamRecords(dataset, ibmSink);
        return null;
     }
}

