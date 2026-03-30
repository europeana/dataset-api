package eu.europeana.api.dataset.oaipmh.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;

public class OAIPMHQueryUtils {

    // oai pmh verb names
    public static final String LIST_RECORDS_VERB  = "ListRecords";

    // query fields
    public static final String SET                = "set";
    public static final String METADATA_PREFIX    = "metadataPrefix";
    public static final String VERB               = "verb";
    // oai pmh ListRecord Response fields
    public static final String RECORD             = "record";
    public static final String HEADER             = "header";
    public static final String IDENTIFIER         = "identifier";
    public static final String METADATA           = "metadata";
    public static final String RESUMPTION_TOKEN   = "resumptionToken";


    // todo check if we need to add harvest-from "from" param
    public static String buildListRecordQuery(String oaipmhUrl, String verb, String metadataPrefix, String setId, String resumptionToken) throws URISyntaxException {
            URIBuilder uriBuilder = new URIBuilder(oaipmhUrl)
                    .addParameter(VERB, verb);
            // If resumptionToken is present, then build resumptionToken rqeuest
            if (StringUtils.isNotEmpty(resumptionToken)) {
                uriBuilder.addParameter(RESUMPTION_TOKEN, resumptionToken);
            } else {
                uriBuilder.addParameter(METADATA_PREFIX, metadataPrefix);
                if (StringUtils.isNotEmpty(setId)) {
                    uriBuilder.addParameter(SET, setId);
                }
            }
            return uriBuilder.build().toString();
    }
}
