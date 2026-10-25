package eu.europeana.api.web;

import eu.europeana.api.auth.DatasetAuthService;
import eu.europeana.api.commons_sb3.error.config.ErrorMessage;
import eu.europeana.api.commons_sb3.error.exceptions.ApplicationAuthenticationException;
import eu.europeana.api.config.DatasetServingConfig;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class for the endpoints exposed by dataset-serving module
 */
@RestController
public class DatasetServingController {

    private static final Logger LOG = LogManager.getLogger(DatasetServingController.class);
    private static final String ZIP_EXTENSION = ".zip";
    private final DatasetAuthService authService;

    private final DatasetServingConfig config;

    /**
     * Constructs a new {@code DatasetServingController}
     * @param authService  service used to handle authorization
     * @param config provides settings for dataset serving
     */
    public DatasetServingController(DatasetAuthService authService,DatasetServingConfig config){
        this.authService= authService;
        this.config = config;
    }

    /**
     * Fetches the downloadable file details grouped by their respective dataset ID. Results can be
     * sorted with {@code sortBy} and {@code sortOrder}.
     *
     * @param sortBy    field to sort by: {@code name}, {@code size}, or {@code lastModified}
     *                  (default {@code name})
     * @param sortOrder {@code asc} or {@code desc} (default {@code asc})
     * @return map containing the datasetID and respective file details list, in the requested order
     */
    @GetMapping(value ={"/dataset/"})
    public ResponseEntity<List<DatasetFileGroup>> getFileList(
        @RequestParam(name = "sortBy", defaultValue = "name") String sortBy,
        @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {

        final FileSortField sortField;
        final SortOrder order;
        try {
            sortField = FileSortField.fromParam(sortBy);
            order = SortOrder.fromParam(sortOrder);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid sort parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        try {
            List<FileDetails> allFiles = new ArrayList<>();

            // look for dataset archive files in each folder associated to a format e.g. XML, TTL
            for (FileTypes extension : FileTypes.values()) {

                String directoryPath = config.getDataSetLocalStoragePath() + extension.label;
                Path directory = Paths.get(directoryPath);
                if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                    LOG.log(Level.ERROR, "Invalid Path {}", directoryPath);
                    continue;
                }

                // Files.list opens a DirectoryStream that must be closed
                try (Stream<Path> list = Files.list(directory)) {
                    list.map(this::getFileDetails)
                        .filter(Objects::nonNull)
                        .forEach(allFiles::add);
                }
            }

            Comparator<FileDetails> comparator = sortField.comparator();
            if (order == SortOrder.DESC) {
                comparator = comparator.reversed();
            }
            allFiles.sort(comparator);

            // LinkedHashMap keeps insertion order from the sorted list
            Map<String, List<FileDetails>> datasetFilesDetails = new LinkedHashMap<>();
            for (FileDetails details : allFiles) {
                datasetFilesDetails
                    .computeIfAbsent(
                        FilenameUtils.getBaseName(details.getFileName()),
                        key -> new ArrayList<>())
                    .add(details);
            }

            //Convert the map into the list of DTOs so that its rendered as an ordered JSON Array []
            List<DatasetFileGroup> orderedList = datasetFilesDetails.entrySet().stream()
                .map(entry -> new DatasetFileGroup(entry.getKey(),entry.getValue()))
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(orderedList);

        } catch (IOException e) {
            LOG.error("Exception while fetching files for download - ", e);
            return ResponseEntity.internalServerError().build();
        }

    }

    /**
     * Fetch the file details
     * @param filePath path to file
     * @return  the details containing zip filename , parent directory name (which is also a type of content the file has),
     *
     */
    private FileDetails getFileDetails(Path filePath) {
        try {

            BasicFileAttributes fileAttr = Files.readAttributes(filePath,
                BasicFileAttributes.class);

            if (!fileAttr.isRegularFile() || !filePath.toString().endsWith(ZIP_EXTENSION)) {
                return null;
            }

            // MIME type from parent folder label (XML → application/rdf+xml, TTL → text/turtle)
            FileTypes fileType = FileTypes.fromLabel(
                FilenameUtils.getBaseName(filePath.getParent().toString()));
            if (fileType == null) {
                return null;
            }

            // Dataset ID from zip name (e.g. 1.zip → 1); display name uses format extension (1.xml / 1.ttl)
            String datasetId = FilenameUtils.getBaseName(filePath.toString());
            String fileName = datasetId + "." + fileType.extension;

            String formatLabel = fileType.extension;

            // Restrict last-modified precision to millis (not nanos)
            // e.g. 2026-03-26T08:15:30.123456789Z -> 2026-03-26T08:15:30.123Z
            String lastModified = fileAttr.lastModifiedTime().toInstant()
                .truncatedTo(ChronoUnit.MILLIS)
                .toString();

            return new FileDetails(
                fileName,
                formatLabel,
                fileType.mediaType,
                //FileUtils.byteCountToDisplaySize(fileAttr.size()),
                fileAttr.size(),
                lastModified,
                "/dataset/" + datasetId + "?format=" + fileType.name().toLowerCase(Locale.ENGLISH));


        } catch (IOException e) {
            LOG.error(String.format("Error while reading details for filePath- %s" , filePath.getFileName()),e.getMessage());
            return null;
        }

    }

    /**
     * Generates the streaming response for dataset zip file to download.
     * @param datasetID numeric id of the dataset
     * @param fileExtension the type of inner file in the zip to download e.g. XML,TTL
     * @param request HttpServletRequest
     * @return response entity containing StreamingResponseBody
     */
    @GetMapping("/dataset/{datasetId}")
    public ResponseEntity<Resource> getFileResource(
        @PathVariable("datasetId") String datasetID,
        @RequestParam(name = "format", defaultValue = "XML", required = false) String fileExtension,
        HttpServletRequest request) throws IOException {

        if (!FileTypes.isValid(fileExtension)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            authorizeReadAccess(request);   // see auth section below
        } catch (ApplicationAuthenticationException e) {
            LOG.error("Unauthorized access ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Path path = Paths.get(getFileToServe(datasetID, fileExtension));
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(path);
        String etag = "\"" + Files.size(path) + "-" + Files.getLastModifiedTime(path).toMillis() + "\"";

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(datasetID + ZIP_EXTENSION).build().toString())
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .eTag(etag)
            .lastModified(Files.getLastModifiedTime(path).toMillis())
            .contentLength(Files.size(path))
            .body(resource);
    }

    private void authorizeReadAccess(HttpServletRequest request) throws ApplicationAuthenticationException {
        if (authService.authorizeReadAccess(request) == null) {
            throw new ApplicationAuthenticationException(ErrorMessage.TOKEN_INVALID_401);
        }
    }

    private String getFileToServe(String datasetID, String fileExtention) throws IOException {
        String filePath = Paths.get(
            config.getDataSetLocalStoragePath(), fileExtention.toUpperCase(Locale.ENGLISH),
            datasetID + ZIP_EXTENSION).toString();

        //validate if file exists
        File file = new File(filePath);
        if(!file.exists()){
            throw new IOException();
        }
        return file.getPath();
    }
}
