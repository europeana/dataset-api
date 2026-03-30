package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.slack.SlackConnection;
import eu.europeana.api.dataset.generation.service.ScheduleDatasetService;
import jakarta.annotation.Resource;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DatasetReportTasklet implements Tasklet {

    @Resource
    ScheduleDatasetService scheduleDatasetService;

    @Resource
    SlackConnection slackConnection;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
       updateDatabase();
       slackConnection.publishStatusReport("datasets generation completed successfully");

       return RepeatStatus.FINISHED;
    }

    /**
     * Updates the database by performing post-processing tasks related to dataset scheduling.
     * The following operations are performed:
     * - Updates the last harvest date to the current date using the ScheduleDatasetService.
     * - Deletes processed datasets from the database to free resources and maintain clean state.
     */
    private void updateDatabase() {
        scheduleDatasetService.updateLastHarvestDate(new Date());
        scheduleDatasetService.cleanUpAfterProcessing();
    }
}
