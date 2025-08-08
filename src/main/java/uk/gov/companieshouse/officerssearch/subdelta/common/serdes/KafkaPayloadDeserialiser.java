package uk.gov.companieshouse.officerssearch.subdelta.common.serdes;

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
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.InvalidPayloadException;

public class KafkaPayloadDeserialiser<T> implements Deserializer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final Class<T> type;

    public KafkaPayloadDeserialiser(Class<T> type) {
        this.type = type;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<T> reader = new ReflectDatumReader<>(type);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            LOGGER.error("Error deserialising message payload.", e);
            throw new InvalidPayloadException(String.format("Invalid payload: [%s] was provided.", new String(data)), e);
        }
    }
}
