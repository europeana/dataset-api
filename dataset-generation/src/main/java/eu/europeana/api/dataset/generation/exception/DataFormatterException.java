package eu.europeana.api.dataset.generation.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

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
