package eu.europeana.api.dataset.generation.service.impl;

//import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
//import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata;

import eu.europeana.api.dataset.generation.service.RecordSink;
import eu.europeana.api.dataset.oaipmh.model.Record;

public class IBMRecordSink implements RecordSink {

    public IBMRecordSink() {

    }
    //private final AmazonS3 cosClient;
   // private final String bucketName;
    private long counter = 0;


//    public IBMRecordSink(AmazonS3 cosClient, String bucketName) {
//       // this.cosClient = cosClient;
//        this.bucketName = bucketName;
//    }

    @Override
    public void consume(Record record) {
        try {
            //String recordId = record.getHeader().getIdentifier();
//            String xml = record.toString();
//
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentLength(xml.getBytes().length);
//            cosClient.putObject(bucketName, recordId + ".xml", new ByteArrayInputStream(xml.getBytes()), metadata);
//
//            counter++;
//            if (counter % 1000 == 0) {
//                System.out.println("Uploaded " + counter + " records to IBM COS...");
//            }

//        } catch (Exception e) {
//            System.err.println("Failed to store record " + record.getHeader().getIdentifier());
//            e.printStackTrace();
//        }
        }finally {

        }
    }

    @Override
    public void close() {
        System.out.println("Total records uploaded to IBM COS: " + counter);
    }
}