package eu.europeana.api.dataset.oaipmh.exception;

public class RetryWithReducedPageSizeException extends Exception {

    private static final long serialVersionUID = 8281933808897246375L;

    /**
     * Constructor for exception to indicate that an error occurred during invocation of the remote
     * service or parsing of service response
     *
     * @param msg              the error message
     */
    public RetryWithReducedPageSizeException(String msg) {
        super(msg);
    }

    /**
     * Constructor for exception to indicate that an error occurred during invocation of the remote
     * service or parsing of service response
     *
     * @param msg              the error message
     * @param t                eventual exception thrown when extracting information from the remote service response
     */
    public RetryWithReducedPageSizeException(String msg, Throwable t) {
        super(msg, t);
    }
}