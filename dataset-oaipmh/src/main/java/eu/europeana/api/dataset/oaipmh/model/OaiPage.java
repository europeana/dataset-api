package eu.europeana.api.dataset.oaipmh.model;

import java.util.List;

/**
 * Represents a single page of results returned by an OAI-PMH query.
 * Each page includes a list of records and an optional resumption token
 * for retrieving the next page.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
public class OaiPage {

    public final List<Record> records;
    public final String resumptionToken;

    public OaiPage(List<Record> records, String resumptionToken) {
        this.records = records;
        this.resumptionToken = resumptionToken;
    }

    public List<Record> getRecords() {
        return records;
    }

    public String getResumptionToken() {
        return resumptionToken;
    }
}
