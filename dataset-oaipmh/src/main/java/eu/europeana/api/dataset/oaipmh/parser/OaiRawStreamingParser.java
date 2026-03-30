package eu.europeana.api.dataset.oaipmh.parser;

import eu.europeana.api.dataset.oaipmh.model.OaiPage;
import eu.europeana.api.dataset.oaipmh.model.Record;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
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
            throws XMLStreamException {

        List<Record> records = new ArrayList<>();
        String resumptionToken = null;

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        XMLEventReader reader = inputFactory.createXMLEventReader(oaiStream);

        String currentIdentifier = null;

        ByteArrayOutputStream baos = null;
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
                    baos = new ByteArrayOutputStream();
                    eventWriter = outputFactory.createXMLEventWriter(baos, StandardCharsets.UTF_8.name());
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

                        InputStream metadataStream = new ByteArrayInputStream(baos.toByteArray());
                        records.add(new Record(currentIdentifier, metadataStream));
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