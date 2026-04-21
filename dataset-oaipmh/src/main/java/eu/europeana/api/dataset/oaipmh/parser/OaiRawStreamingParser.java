package eu.europeana.api.dataset.oaipmh.parser;

import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.model.Record;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.dom.DOMResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static eu.europeana.api.dataset.oaipmh.utils.OAIPMHQueryUtils.*;

/**
 * The OaiRawStreamingParser class provides functionality to parse an OAI-PMH
 * response and stream out the metadata of each record while also handling the
 * retrieval of the resumption token for subsequent requests.
 *
 * we can also load it into Jena later with Model.read(InputStream, null)
 *
 * @author Srishti Singh
 * @since 23 March 2026
 */
public class OaiRawStreamingParser {

    /**
     * Parses OAI-PMH response and streams <metadata>, also fetches resumptionToken.
     */
    public static OaiPage parseOaiResponse(InputStream oaiStream)
            throws XMLStreamException, ParserConfigurationException {

        List<Record> records = new ArrayList<>();
        String resumptionToken = null;

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLEventReader reader = inputFactory.createXMLEventReader(oaiStream);

        String currentIdentifier = null;

        Document document = null;
        XMLEventWriter eventWriter = null;

        boolean inMetadata = false;
        boolean inHeader = false;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                String localName = start.getName().getLocalPart();

                if (RECORD.equals(localName)) {
                    currentIdentifier = null;
                }

                if (HEADER.equals(localName)) {
                    inHeader = true;
                }

                if (IDENTIFIER.equals(localName) && inHeader) {
                    XMLEvent next = reader.nextEvent();
                    if (next.isCharacters()) {
                        currentIdentifier = next.asCharacters().getData();
                    }
                }

                if (METADATA.equals(localName)) {
                    inMetadata = true;
                    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                    eventWriter = XMLOutputFactory.newInstance().createXMLEventWriter(new DOMResult(document));
                   // eventWriter.add(event); // write <metadata> EXACT, we don't want to write <metadata> string
                    continue;
                }

                if (inMetadata) {
                    eventWriter.add(event); // copy EXACT event
                }

                if (RESUMPTION_TOKEN.equals(localName)) {
                    XMLEvent next = reader.nextEvent();
                    if (next.isCharacters()) {
                        resumptionToken = next.asCharacters().getData();
                    }
                }
            }

            else if (event.isCharacters() && inMetadata) {
                eventWriter.add(event); // exact text
            }

            else if (event.isEndElement()) {
                EndElement end = event.asEndElement();
                String localName = end.getName().getLocalPart();

                if (HEADER.equals(localName)) {
                    inHeader = false;
                }

                if (inMetadata) {
                    if (METADATA.equals(localName)) {
                        eventWriter.flush();
                        eventWriter.close();
                        records.add(new Record(currentIdentifier, document));
                        inMetadata = false;
                    } else {
                        eventWriter.add(event);
                    }
                }
            }
        }
        reader.close();
        return new OaiPage(records, resumptionToken);
    }
}