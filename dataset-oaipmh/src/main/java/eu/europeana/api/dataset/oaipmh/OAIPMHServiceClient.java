package eu.europeana.api.dataset.oaipmh;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons_sb3.http.HttpConnection;
import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.parser.OaiRawStreamingParser;
import eu.europeana.oaipmh.model.response.OAIResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.XML;

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

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Executes oai request and returns the requested OAIResponse class response
     * @param request request for oai pmh service
     * @param responseClass response class for the oai response
     * @return OAIResponse
     */
    public OAIResponse executeOaiRequest(String request, Class<? extends OAIResponse> responseClass) {
        try {
            ClassicHttpResponse response = oaiPmhClient.get(request, Collections.singletonMap(HttpHeaders.ACCEPT, "application/xml"), null);
            if (response.getCode() == HttpStatus.SC_OK) {
                String json = XML.toJSONObject(EntityUtils.toString(response.getEntity())).toString();
                return mapper.readValue(json, responseClass);
            }
        } catch (IOException | ParseException e) {
            LOG.error("Exception while getting the response from oai pmh.", e);
        }
        return  null;
    }

    /**
     * Executes an OAI-PMH request and retrieves the response as an {@link OaiPage} object.
     *
     * @param request the URL or query string representing the OAI-PMH request to be executed.
     * @return an {@link OaiPage} containing the parsed records and resumption token if the request is successful,
     *         or {@code null} if an exception occurs or if the response is not successful.
     */
    public OaiPage executeAndGetResponse(String request) {
        try {
            ClassicHttpResponse response = oaiPmhClient.get(request, Collections.singletonMap(HttpHeaders.ACCEPT, "application/xml"), null);

            if (response.getCode() == HttpStatus.SC_OK) {
                return OaiRawStreamingParser.parseOaiResponse(response.getEntity().getContent());
            }
        } catch (IOException | XMLStreamException e) {
            LOG.error("Exception while getting the response from oai pmh.", e);
        }
        return  null;
    }
}
