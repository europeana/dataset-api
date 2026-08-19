package eu.europeana.api.web;

import eu.europeana.api.auth.DatasetAuthService;
import eu.europeana.api.commons_sb3.error.config.ErrorMessage;
import eu.europeana.api.commons_sb3.error.exceptions.ApplicationAuthenticationException;
import eu.europeana.api.config.DatasetServingConfig;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Controller class for the endpoints exposed by dataset-serving module
 */
@RestController
public class DatasetServingController {

    private static final Logger LOG = LogManager.getLogger(DatasetServingController.class);
    private static final String ZIP_EXTENSION = ".zip";
    private static final int BUFFER_SIZE = 8192; // Increased from 1024 to 8KB for faster throughput

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
     * Fetches the downloadable file details grouped by their respective dataset ID     *
     * @return map containing the datasetID and respective file details list
     */
    @GetMapping(value ={"/dataset/"})
    public ResponseEntity<Map<String, List<FileDetails>>> getFileList(){

        try {
            Map<String,List<FileDetails>> datasetFilesDetails = new HashMap<>();

             //look for dataset archive files in each folder which is associated to file content type e.g. XML,TTL
             for(FileTypes extension  : FileTypes.values()){

                 String directoryPath = config.getDataSetLocalStoragePath() + extension.label;
                 Path directory = Paths.get(directoryPath);
                 if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                     LOG.log(Level.ERROR,"Invalid Path {}", directoryPath);
                     continue;
                 }

                 //If the zip file (<datasetID>.zip) is found  , put it in the response map against that datasetID
                 // Files.list opens the DirectoryStream internally which needs to be closed.

                 try (Stream<Path> list = Files.list(directory)) {
                     list.map(this::getFileDetails)
                         .filter(Objects::nonNull)
                         .forEach(details ->
                             datasetFilesDetails.computeIfAbsent(
                                     details.getFileName(),
                                     key -> new ArrayList<>())
                                 .add(details)
                         );
                 }
             }
            return ResponseEntity.ok().body(datasetFilesDetails);

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

            String type = FileTypes.getTypeByLabel(
                FilenameUtils.getBaseName(filePath.getParent().toString()));

            String baseName = FilenameUtils.getBaseName(filePath.toString());
            return new FileDetails(
                baseName,
                type,
                FileUtils.byteCountToDisplaySize(fileAttr.size()),
                fileAttr.lastModifiedTime().toInstant().toString(),
                "/dataset/"+baseName+"?format="+type.toLowerCase(Locale.ENGLISH));


        } catch (IOException e) {
            LOG.error(String.format("Error while reading details for filePath- %s" , filePath.getFileName()),e.getMessage());
            return null;
        }

    }

    /**
     * Generates the streaming response for dataset zip file to download.
     * @param datasetID numeric id of the dataset
     * @param fileExtension the type of inner file in the zip to download e.g. XML,TTL
     * @param rangeHeader range headers provided in case the download is paused and then resumed
     * @param request HttpServletRequest
     * @return response entity containing StreamingResponseBody
     */
    @GetMapping("/dataset/{datasetId}")
    public ResponseEntity<StreamingResponseBody> getFileResource(@PathVariable("datasetId") String datasetID,
        @RequestParam(name = "format",defaultValue ="XML",required = false) String fileExtension,
        @RequestHeader(value = "Range", required = false) String rangeHeader,
        HttpServletRequest request) {
        try {
            if (!FileTypes.isValid(fileExtension)) {
                return ResponseEntity.badRequest().build();
            }
            LOG.info("Range Header value : {}",rangeHeader);
            // Only authorized users can download
            authorizeReadAccess(request);

            return generateResponse(datasetID, fileExtension, rangeHeader);

        } catch (ApplicationAuthenticationException e) {
            LOG.error("Unauthorized access ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IOException e) {
            LOG.error("Exception while fetching the file ", e);
            return ResponseEntity.notFound().build();
        }
    }

//    private ResponseEntity<StreamingResponseBody> generateResponse(
//        String datasetID, String fileExtension, String rangeHeader) throws IOException {
//
//        StreamingResponseBody responseStream;
//        String filePathString = getFileToServe(datasetID, fileExtension);
//        Path path = Paths.get(filePathString);
//        Long fileSize = Files.size(path);
//
//
//        String etag = "\"" + fileSize + "-" + Files.getLastModifiedTime(path) + "\"";
//
//        byte[] buffer = new byte[BUFFER_SIZE];
//        final HttpHeaders responseHeaders = new HttpHeaders();
//
//        if (rangeHeader == null) {
//            updateResponseHeaders(datasetID, responseHeaders, fileSize.toString(),etag);
//
//            responseStream = os -> {
//                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
//                try (file) {
//                    long pos = 0;
//                    file.seek(pos);
//                    while (pos < fileSize - 1) {
//                        file.read(buffer);
//                        os.write(buffer);
//                        pos += buffer.length;
//                    }
//                    os.flush();
//                }
//            };
//            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK);
//        }
//
//        //If the Range headers are specified e.g. 'bytes=500-1000' calculate ranges
//        String rangeValue = rangeHeader.replace("bytes=", "");
//        String[] ranges = rangeValue.split("-");
//        Long rangeStart = Long.parseLong(ranges[0]);
//        Long rangeEnd = calculateRangeEnd(ranges, fileSize);
//        String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
//        updateResponseHeaders(datasetID, responseHeaders, contentLength,etag);
//
//        responseHeaders.add(HttpHeaders.CONTENT_RANGE, "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);
//
//        final Long rangeEndVal = rangeEnd;
//        responseStream = os -> {
//            RandomAccessFile file = new RandomAccessFile(filePathString, "r");
//            try (file) {
//                long pos = rangeStart;
//                file.seek(pos);
//                while (pos < rangeEndVal) {
//                    file.read(buffer);
//                    os.write(buffer);
//                    pos += buffer.length;
//                }
//                os.flush();
//            }
//        };
//        return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
//    }

    private ResponseEntity<StreamingResponseBody> generateResponse(
        String datasetID, String fileExtension, String rangeHeader) throws IOException {

        StreamingResponseBody responseStream;
        String filePathString = getFileToServe(datasetID, fileExtension);
        Path path = Paths.get(filePathString);
        Long fileSize = Files.size(path);

        String etag = "\"" + fileSize + "-" + Files.getLastModifiedTime(path) + "\"";
        final HttpHeaders responseHeaders = new HttpHeaders();

        if (rangeHeader == null) {
            updateResponseHeaders(datasetID, responseHeaders, fileSize.toString(), etag);

            responseStream = os -> {
                try (RandomAccessFile file = new RandomAccessFile(filePathString, "r")) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long pos = 0;
                    int bytesRead;
                    // Properly track bytesRead to avoid corrupting or padding the file end
                    while (pos < fileSize && (bytesRead = file.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        pos += bytesRead;
                    }
                    os.flush();
                }
            };
            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK);
        }

        // If Range headers are specified
        String rangeValue = rangeHeader.replace("bytes=", "");
        String[] ranges = rangeValue.split("-");
        Long rangeStart = Long.parseLong(ranges[0]);
        Long rangeEnd = calculateRangeEnd(ranges, fileSize);
        long contentLength = (rangeEnd - rangeStart) + 1;

        updateResponseHeaders(datasetID, responseHeaders, String.valueOf(contentLength), etag);
        responseHeaders.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize);

        responseStream = os -> {
            try (RandomAccessFile file = new RandomAccessFile(filePathString, "r")) {
                file.seek(rangeStart);
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesRemaining = contentLength;
                int bytesRead;

                while (bytesRemaining > 0 && (bytesRead = file.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                    os.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
                os.flush();
            }
        };
        return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
    }

    private Long calculateRangeEnd(String[] ranges, Long fileSize) {
        Long rangeEnd;
        if (ranges.length > 1) {
            rangeEnd = Long.parseLong(ranges[1]);
        } else {
            rangeEnd = fileSize - 1;
        }
        if (fileSize < rangeEnd) {
            rangeEnd = fileSize - 1;
        }
        return rangeEnd;
    }

    private static void updateResponseHeaders(String datasetID, HttpHeaders responseHeaders,
        String contentLength,String etag) {
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, "application/zip");
        responseHeaders.add(HttpHeaders.CONTENT_LENGTH, contentLength);
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + datasetID + ZIP_EXTENSION
            + "\"");
        responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        responseHeaders.add(HttpHeaders.ETAG,etag);
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
