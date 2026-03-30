package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ListRecordService;
import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.generation.service.impl.MultiRecordSink;
import eu.europeana.api.dataset.generation.service.impl.ZipRecordSink;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.DATA_FORMATS_BEAN;
import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.OAI_PMH_ZIP_PROCESSOR;

/**
 * A processor for harvesting OAI-PMH records from a given dataset and storing them
 * as a ZIP archive. This class uses the {@link ListRecordService} to stream records
 * and writes them into a ZIP file using {@link ZipRecordSink}.
 *
 * This processor acts as the integration point between the record harvesting service
 * and the process of archiving records to disk. It ensures efficient handling of potentially
 * large datasets by using a streaming approach, avoiding memory overhead.
 *
 * @author Srishti Singh
 * @since 12 March 2026
 */
@Component(OAI_PMH_ZIP_PROCESSOR)
public class OaiPmhZipProcessor extends BaseProcessor {

    @Resource
    GeneratorSettings settings;

    @Resource
    private ListRecordService listRecordService;

    @Resource(name = DATA_FORMATS_BEAN)
    private Map<RdfFormat, DataFormatter> formats;

    @Override
    ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception {

        List<RecordSink> sinks = new ArrayList<>();

        for (var entry : formats.entrySet()) {
            File zipFile = new File(createFolder(entry.getKey().name()), dataset.getDatasetId() + ".zip");
            sinks.add(new ZipRecordSink(zipFile, entry.getValue()));
        }

        try (MultiRecordSink multiSink = new MultiRecordSink(sinks)) {
            listRecordService.streamRecords(dataset, multiSink);
        }

        return dataset;
    }

    /**
     * Creates a new folder within the datasets directory if it does not already exist.
     *
     * @param folderName the name of the folder to be created
     * @return the {@link File} object representing the folder
     */
    private File createFolder(String folderName) {
        File folder =  new File(settings.getDatasetsFolder(), folderName);

        if (!folder.exists() ) {
            folder.mkdirs();
        }
        return  folder;
    }
}

