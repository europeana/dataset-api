package eu.europeana.api.dataset.generation.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * This exception is thrown to indicate errors specific to the Search API client interactions.
 * It extends the {@link EuropeanaApiException} to represent application-level exceptions
 * associated with the Search API's usage.
 */
public class SearchApiClientException extends EuropeanaApiException {
    private static final long serialVersionUID = 1L;

    public SearchApiClientException(String msg) {
        super(msg);
    }

    public SearchApiClientException(String msg, Throwable cause) {
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
