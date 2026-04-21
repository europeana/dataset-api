package eu.europeana.api.dataset.generation.writer;

import eu.europeana.api.dataset.generation.model.ScheduledDataset;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;

import org.jspecify.annotations.NonNull;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.SCHEDULED_DATASET_WRITER;

@Component(SCHEDULED_DATASET_WRITER)
public class ScheduledDatasetWriter implements ItemWriter<ScheduledDataset> {

    @Resource
    ScheduleDatasetService scheduleDatasetService;

    @Override
    public void write(@NonNull Chunk<? extends ScheduledDataset> chunk) throws Exception {
        // for now do nothing

    }
}
