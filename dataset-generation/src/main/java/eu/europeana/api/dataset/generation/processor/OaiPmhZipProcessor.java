package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ListRecordService;
import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import eu.europeana.api.dataset.generation.service.impl.MultiRecordSink;
import eu.europeana.api.dataset.generation.service.impl.ZipRecordSink;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Resource
    ScheduleDatasetService scheduleDatasetService;

    @Resource(name = DATA_FORMATS_BEAN)
    private Map<RdfFormat, DataFormatter> formats;

    @Override
    ScheduledDataset doProcessing(ScheduledDataset dataset) throws Exception {

        List<RecordSink> sinks = new ArrayList<>();
        List<Path> tempFiles = new ArrayList<>();
        List<Path> targetFiles = new ArrayList<>();

        for (var entry : formats.entrySet()) {
            Path dir = createFolder(entry.getKey()).toPath();

            Path target = dir.resolve(dataset.getDatasetId() + ".zip");
            Path temp = Files.createTempFile(dir, dataset.getDatasetId() + ".", ".zip.tmp");

            tempFiles.add(temp);
            targetFiles.add(target);

            sinks.add(new ZipRecordSink(dataset.getDatasetId(), temp.toFile(), entry.getValue()));
        }

        try (MultiRecordSink multiSink = new MultiRecordSink(sinks)) {
            long failedRecords = listRecordService.streamRecords(dataset, multiSink);
            scheduleDatasetService.updateFailedRecords(dataset, failedRecords); // update failedRecords and hasBeenProcessed
        }

        // 🔥 Atomic swap happens ONLY after all sinks are closed
        performAtomicSwap(tempFiles, targetFiles);

        return dataset;
    }

    /**
     * Creates a new folder within the datasets directory if it does not already exist.
     *
     * @param rdfFormat  rdf format of the folder
     * @return the {@link File} object representing the folder
     */
    private File createFolder(RdfFormat rdfFormat) {
        File folder =  new File(settings.getDatasetsFolder(), getFolderName(rdfFormat));

        if (!folder.exists() ) {
            folder.mkdirs();
        }
        return  folder;
    }

    /**
     * Determines the folder name based on the provided RDF format.
     * If the RDF format is XML, the folder name corresponds to the format's name.
     * Otherwise, the alternative name of the RDF format is used in uppercase.
     *
     * This is to synchronize the turtle format folder name to TTL and not TURTLE
     * @param rdfFormat the RDF format used to determine the folder name
     * @return the folder name as a string, based on the given RDF format
     */
    private String getFolderName(RdfFormat rdfFormat) {
        return rdfFormat.equals(RdfFormat.XML) ?  rdfFormat.name() : rdfFormat.getExtension().toUpperCase(Locale.ROOT);
    }

}

