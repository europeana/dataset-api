package eu.europeana.api.web;

/**
 * Encapsulates details related to downloadable file e.g. name ,size ,creation date
 */
public class FileDetails {

    private String fileName;
    private String contentType;
    private String fileSize;
    private String lastModified;
    private String downloadURL;


    /**
     * Initialize file details
     * @param fileName
     * @param contentType
     * @param fileSize
     * @param lastModified
     * @param url
     */
    public FileDetails(String fileName, String contentType,String fileSize, String lastModified,String url) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.downloadURL = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

}
