package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class ResourceChangedDataSerialiser implements Serializer<ResourceChangedData> {

    @Override
    public byte[] serialize(String topic, ResourceChangedData data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException e) {
            throw new NonRetryableException("Error serialising delta", e);
        }
        return outputStream.toByteArray();
    }

    DatumWriter<ResourceChangedData> getDatumWriter() {
        return new ReflectDatumWriter<>(ResourceChangedData.class);
    }
}
