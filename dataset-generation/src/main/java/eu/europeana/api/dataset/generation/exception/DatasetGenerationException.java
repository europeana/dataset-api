package eu.europeana.api.dataset.generation.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * A custom exception that represents errors encountered during the dataset generation process.
 *
 * This exception extends {@link EuropeanaApiException} and is used to capture
 * application-specific issues that arise while generating datasets. It provides a structured
 * way to handle errors related to dataset creation, such as failures in data processing
 * or resource generation activities.
 */
public class DatasetGenerationException extends EuropeanaApiException {
    private static final long serialVersionUID = 1L;

    public DatasetGenerationException(String msg) {
        super(msg);
    }

    public DatasetGenerationException(String msg, Throwable cause) {
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
