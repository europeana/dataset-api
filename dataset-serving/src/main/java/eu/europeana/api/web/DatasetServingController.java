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
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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

    public static Logger LOG = LogManager.getLogger(DatasetServingController.class);

    private DatasetAuthService authService;

    private DatasetServingConfig config;

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
                      LOG.error("Invalid Path "+directoryPath);
                     continue;
                 }

                 //If the zip file (<datasetID>.zip) is found  , put it in the response map against that datasetID
                 // Files.list opens the DirectoryStream internally which needs to be closed.
                 try (Stream<Path> list = Files.list(directory)) {
                     list.forEach(path -> {
                         FileDetails details = getFileDetails(path);
                         if (details != null) {
                             datasetFilesDetails.computeIfAbsent(
                                     details.getFileName(),
                                     key -> new ArrayList<>())
                                 .add(details);
                         }
                     });
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

            if (!fileAttr.isRegularFile() || !filePath.toString().endsWith(".zip")) {
                return null;
            }

            String type = FileTypes.getTypeByLabel(
                FilenameUtils.getBaseName(filePath.getParent().toString()));

            String baseName = FilenameUtils.getBaseName(filePath.toString());
            return new FileDetails(
                //filePath.getFileName().toString(),
                baseName,
                type,
                FileUtils.byteCountToDisplaySize(fileAttr.size()),
                fileAttr.lastModifiedTime().toInstant().toString(),
                "/dataset/"+baseName+"?format="+type.toLowerCase(Locale.ENGLISH));


        } catch (IOException e) {
            LOG.error("Error while reading details for filePath- " + filePath.getFileName());
            return null;
        }

    }


    @GetMapping("/dataset/{datasetId}")
    public ResponseEntity<StreamingResponseBody> getFileResource(@PathVariable("datasetId") String datasetID,
        @RequestParam(name = "format",defaultValue ="XML",required = false) String fileExtension,
        @RequestHeader(value = "Range", required = false) String rangeHeader,
        HttpServletRequest request) {
        try {
            // validate file extension;
            if (!FileTypes.isValid(fileExtension)) {
                return ResponseEntity.badRequest().build();
            }
            //validate authentication token
            authorizeReadAccess(request);
            // Serve file resource

            // return getResourceRegionResponse(null, getFileToServe(datasetID, fileExtension));

            StreamingResponseBody responseStream;
            String filePathString = getFileToServe(datasetID, fileExtension);
            Path filePath = Paths.get(filePathString);
            Long fileSize = Files.size(filePath);
            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null) {
                responseHeaders.add("Content-Type", "application/zip");
                responseHeaders.add("Content-Length", fileSize.toString());
                responseHeaders.add("Content-Disposition","attachment; filename=\"" + datasetID+".zip" + "\"");

                responseStream = os -> {
                    RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                    try (file) {
                        long pos = 0;
                        file.seek(pos);
                        while (pos < fileSize - 1) {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch (Exception e) {}
                };
                return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = fileSize - 1;
            }
            if (fileSize < rangeEnd) {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", "application/zip");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Content-Disposition","attachment; filename=\"" + datasetID+".zip" + "\"");
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);

            final Long _rangeEnd = rangeEnd;
            responseStream = os -> {
                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                try (file) {
                    long pos = rangeStart;
                    file.seek(pos);
                    while (pos < _rangeEnd) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                } catch (Exception e) {}
            };
            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);



        } catch (ApplicationAuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void authorizeReadAccess(HttpServletRequest request) throws ApplicationAuthenticationException {
        if (request.getHeader("Authorization") == null
            || authService.authorizeReadAccess(request) == null) {
            throw new ApplicationAuthenticationException(ErrorMessage.TOKEN_INVALID_401);
        }
    }


    private String getFileToServe(String datasetID, String fileExtention) throws IOException {
        String filePath =
            config.getDataSetLocalStoragePath() + "/" + fileExtention.toUpperCase() + "/"
                + datasetID + ".zip";
        //validate if file exists
        File file = new File(filePath);
        if(!file.exists()){
            throw new IOException();
        }
        return file.getPath();
    }

    private ResponseEntity<ResourceRegion> getResourceRegionResponse(
        HttpHeaders headers, File file) throws IOException {
        FileSystemResource resource = new FileSystemResource(file);
        long contentLength = resource.contentLength();
        long smallestChunkSizeInBytes = 1024 * 1024L;

        //check if the range header is specified .Download only that much data.
        HttpRange range = (headers.getRange().isEmpty() ?null: headers.getRange().get(0));
        if(range!=null){
            long startPos = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(smallestChunkSizeInBytes,end-startPos+1);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(rangeLength)
                .body(new ResourceRegion(resource, 0, smallestChunkSizeInBytes));
        }
        //Send full file if it's less than the smallestChunk Size
        else{
            long fullRangeLength = Math.min(smallestChunkSizeInBytes,contentLength);
            return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(fullRangeLength)
                .body(new ResourceRegion(resource, 0, fullRangeLength));
        }
    }

}
