package eu.europeana.api.dataset.generation.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * A custom exception that represents errors encountered during the data formatting process.
 *
 * This exception serves as a specific type of {@link EuropeanaApiException} used to indicate
 * issues related to formatting metadata in different formats (e.g., XML, Turtle). It is typically
 * thrown when an error occurs during the transformation or output writing process.
 */
public class DataFormatterException extends EuropeanaApiException {
    private static final long serialVersionUID = 1L;

    public DataFormatterException(String msg) {
        super(msg);
    }

    public DataFormatterException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public boolean doExposeMessage() {
        return false;
    }

    @Override
    public boolean doLog() {
        return true;
    }

    @Override
    public boolean doLogStacktrace() {
        return false;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
