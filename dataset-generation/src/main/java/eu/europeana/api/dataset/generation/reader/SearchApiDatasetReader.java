package eu.europeana.api.dataset.generation.reader;

import eu.europeana.api.commons_sb3.auth.AuthenticationHandler;
import eu.europeana.api.commons_sb3.definitions.utils.DateUtils;
import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.commons_sb3.error.exceptions.InvalidParamException;
import eu.europeana.api.commons_sb3.http.HttpConnection;
import eu.europeana.api.dataset.generation.config.GeneratorSettings;
import eu.europeana.api.dataset.generation.exception.SearchApiClientException;
import eu.europeana.api.dataset.generation.model.Dataset;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.SEARCH_RECORD_AUTH_HANDLER;
import static  eu.europeana.api.dataset.generation.utils.ModelConstants.facets;
/**
 * This class is responsible for reading datasets from the search API.
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Service
public class SearchApiDatasetReader {

    private static final Logger LOG = LogManager.getLogger(SearchApiDatasetReader.class);

    private static final String DATE_RANGE_TEMPLATE = "%s:[%s TO %s]";

    @Resource
    GeneratorSettings settings;

    @Resource(name = SEARCH_RECORD_AUTH_HANDLER)
    AuthenticationHandler srApiAuthHandler;

    HttpConnection searchApiClient = new HttpConnection(true);

    /**
     * Reads datasets from the search API based on the provided timestamp update.
     * @param timestampUpdate The timestamp update to filter datasets.
     * @return List of datasets retrieved from the search API.
     * @throws EuropeanaApiException If there's an error during API interaction.
     */
    public List<Dataset> getDataset(Date timestampUpdate) throws EuropeanaApiException {
        try (CloseableHttpResponse response = searchApiClient.get(
                buildSearchApiUrl(timestampUpdate),
                Collections.singletonMap(HttpHeaders.ACCEPT, "application/json"),
                srApiAuthHandler)) {

            List<Dataset> datasets = new ArrayList<>();

            // process facet map
            if (response.getCode() == HttpStatus.SC_OK) {
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
                if (jsonObject.has(facets)) {
                    JSONArray fields = jsonObject.getJSONArray(facets).getJSONObject(0).getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject field = fields.getJSONObject(i);
                        //set names from solr are in the form of <setId>_<setName> so we split
                        datasets.add(new Dataset(
                                StringUtils.substringBefore(field.getString("label"), "_"),
                                field.getLong("count")));
                    }
                }
            } else {
                JSONObject jsonObject = new JSONObject(response.getEntity().getContent());
                throw new SearchApiClientException("Exception while getting datasets from search api - " + jsonObject.get("error"));
            }
            LOG.info("Found {} datasets modified after {}", datasets.size(), timestampUpdate);
            return datasets;
        } catch (IOException | ParseException e) {
            throw new SearchApiClientException("Exception while accessing Serach API." + e.getMessage(), e);
        }
    }

    /**
     * Builds the search API URL with the given parameters.
     * <p>
     * https://api.europeana.eu/record/search.json?qf=timestamp_update:[from TO *]&wskey=&
     * query=*&profile=facets&f.edm_datasetName.facet.limit=5000&facet=edm_datasetName&rows=0
     *
     * @param timestampUpdate an optional timestamp string used to filter results by `timestamp_update`;
     *                        if not provided or empty, this parameter will be excluded from the URL.
     *                        Should be in format - 2026-03-03T15:41:41.047Z
     * @return a string representing the constructed search API URL.
     * @throws InvalidParamException if an error occurs while constructing the URL, typically due to an invalid syntax or configuration.
     */
    public String buildSearchApiUrl(Date timestampUpdate) throws InvalidParamException {
        try {
            URIBuilder uriBuilder = new URIBuilder(settings.getSearchApiUrl())
                    .addParameter("query", "*")
                    .addParameter("profile", facets)
                    .addParameter("facet", "edm_datasetName")
                    .addParameter("rows", "0")
                    .addParameter("f.edm_datasetName.facet.limit", "5000");

            if (timestampUpdate != null) {
                uriBuilder.addParameter("qf", String.format(DATE_RANGE_TEMPLATE, "timestamp_update",
                        DateUtils.getZonedDateTime(timestampUpdate), "*"));
            }
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new InvalidParamException(Arrays.asList("search api url", "", e.getMessage()), e);
        }
    }
}
