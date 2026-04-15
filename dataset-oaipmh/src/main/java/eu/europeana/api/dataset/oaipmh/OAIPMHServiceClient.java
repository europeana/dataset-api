package eu.europeana.api.dataset.oaipmh;

import eu.europeana.api.commons_sb3.http.HttpConnection;
import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.parser.OaiRawStreamingParser;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;

/**
 * Provides functionality to interact with an OAI-PMH service to execute requests and retrieve responses
 * in the form of predefined OAIResponse classes.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
public class OAIPMHServiceClient {

    private static final Logger LOG = LogManager.getLogger(OAIPMHServiceClient.class);

    private final HttpConnection oaiPmhClient = new HttpConnection(true);

    /**
     * Executes an OAI-PMH request and retrieves the response as an {@link OaiPage} object.
     *
     * @param request the URL or query string representing the OAI-PMH request to be executed.
     * @return an {@link OaiPage} containing the parsed records and resumption token if the request is successful,
     *         or {@code null} if an exception occurs or if the response is not successful.
     */
    public OaiPage executeAndGetResponse(String request) {
        try (CloseableHttpResponse response =  oaiPmhClient.get(request, Collections.singletonMap(HttpHeaders.ACCEPT, "application/xml"), null)) {
            if (response.getCode() == HttpStatus.SC_OK) {
                return OaiRawStreamingParser.parseOaiResponse(response.getEntity().getContent());
            }
        } catch (IOException | XMLStreamException | ParserConfigurationException e) {
            LOG.error("Exception while getting the response from oai pmh.", e);
        }
        return  null;
    }
}
