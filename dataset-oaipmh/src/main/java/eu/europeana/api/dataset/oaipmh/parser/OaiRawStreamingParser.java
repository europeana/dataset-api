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
 * <p>
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
                    currentIdentifier = readElementText(reader);
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
                    resumptionToken = getResumptionToken(reader);
                }
            } else if (event.isCharacters() && inMetadata) {
                eventWriter.add(event); // exact text
            } else if (event.isEndElement()) {
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

    /**
     * Reads and returns the text content of an XML element from the provided XML event reader.
     * The method iterates through the events, extracting character data until the corresponding
     * end element is encountered.
     *
     * @param reader the XML event reader used to read the XML content
     * @return the trimmed text content of the XML element
     * @throws XMLStreamException if an error occurs while reading from the XML event reader
     */
    private static String readElementText(XMLEventReader reader) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();

        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();

            if (e.isCharacters()) {
                sb.append(e.asCharacters().getData());
            }

            if (e.isEndElement()
                    && IDENTIFIER.equals(e.asEndElement().getName().getLocalPart())) {
                break;
            }
        }

        return sb.toString().trim();
    }

    /**
     * Extracts the resumption token from the XML stream.
     * In StAX: <resumptionToken> content is not guaranteed to be a single Characters event
     * It can be split into multiple chunks
     *
     * @param reader xml event reader
     * @return resumption token
     * @throws XMLStreamException if there is any exception
     */
    private static String getResumptionToken(XMLEventReader reader) throws XMLStreamException {
        StringBuilder tokenBuilder = new StringBuilder();
        while (reader.hasNext()) {
            XMLEvent tokenEvent = reader.nextEvent();

            if (tokenEvent.isCharacters()) {
                tokenBuilder.append(tokenEvent.asCharacters().getData());
            }

            if (tokenEvent.isEndElement()
                    && RESUMPTION_TOKEN.equals(tokenEvent.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
        return tokenBuilder.toString().trim();
    }
}