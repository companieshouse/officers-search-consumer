package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;

public class OfficerMergeDataSerialiser implements Serializer<OfficerMergeData> {

    @Override
    public byte[] serialize(String topic, OfficerMergeData data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<OfficerMergeData> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException e) {
            throw new NonRetryableException("Error serialising delta", e);
        }
        return outputStream.toByteArray();
    }

    DatumWriter<OfficerMergeData> getDatumWriter() {
        return new ReflectDatumWriter<>(OfficerMergeData.class);
    }
}
