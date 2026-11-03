package eu.europeana.api.dataset.generation.processor;

import eu.europeana.api.commons_sb3.definitions.format.RdfFormat;
import eu.europeana.api.dataset.generation.format.DataFormatter;
import eu.europeana.api.dataset.generation.utils.CheckSumUtil;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

public class CheckSumGenerationTasklet extends TaskletSupport implements Tasklet {

    private final String storagePath;
    private final Map<RdfFormat, DataFormatter> formats;
    private final boolean checkSumEnabled;

    public CheckSumGenerationTasklet(String storagePath, Map<RdfFormat, DataFormatter> formats, boolean checkSumEnabled) {
        this.storagePath = storagePath;
        this.formats = formats;
        this.checkSumEnabled = checkSumEnabled;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        if (!checkSumEnabled) {
            LOG.info("Checksum generation is disabled. Skipping checksum generation tasklet.");
            return RepeatStatus.FINISHED;
        }

        LOG.info("Starting check sum generation tasklet...");
        long start = System.currentTimeMillis();

        formats.keySet().forEach(format -> {
            LOG.info("Generating checksums for format {}", format);
            Path directory = Paths.get(storagePath).resolve(TaskletSupport.getFolderName(format));
            try {
                generateChecksums(directory);
            } catch (IOException e) {
                LOG.error("Failed to generate checksum for format {} " + format, e);
            }
        });

        LOG.info("Generated Checksum in {} ms", System.currentTimeMillis() - start);
        return RepeatStatus.FINISHED;
    }

    public void generateChecksums(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .forEach(path -> {
                        try {
                            CheckSumUtil.createMd5File(path);
                        } catch (IOException e) {
                            LOG.error("Failed to generate checksum for {}" , path, e);
                        }
                    });
        }
    }
}
