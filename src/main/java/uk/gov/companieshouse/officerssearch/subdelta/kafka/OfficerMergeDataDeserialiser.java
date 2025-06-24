package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.io.IOException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.exception.InvalidPayloadException;

public class OfficerMergeDataDeserialiser implements Deserializer<OfficerMergeData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public OfficerMergeData deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<OfficerMergeData> reader = new ReflectDatumReader<>(
                    OfficerMergeData.class);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            LOGGER.error("Error deserialising message.", e);
            throw new InvalidPayloadException(
                    String.format("Invalid payload: [%s] was provided.", new String(data)), e);
        }
    }
}
