package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.gov.companieshouse.officerssearch.subdelta.exception.InvalidPayloadException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

class ResourceChangedDataDeserialiserTest {

    @Test
    @DisplayName("Deserialise a ResourceChangedData serialised as Avro")
    void testDeserialiseDelta() throws IOException {
        try (ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser()) {

            // given
            ResourceChangedData changeData = new ResourceChangedData("resource_kind",
                    "resource_uri", "context_id", "resource_id", "data",
                    new EventRecord("published_at", "event_type", Collections.emptyList()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                    ResourceChangedData.class);
            writer.write(changeData, encoder);

            // when
            ResourceChangedData actual = deserialiser.deserialize("topic",
                    outputStream.toByteArray());

            // then
            assertThat(actual, is(equalTo(changeData)));
        }
    }

    @Test
    @DisplayName("Throws InvalidPayloadException if IOException encountered when deserialising a message")
    void testDeserialiseDataThrowsInvalidPayloadExceptionlIfIOExceptionEncountered()
            throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        try(ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser()) {

            // when
            Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());

            // then
            InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
            // Note the '\n' is the length prefix of the invalid data sent to the deserialiser
            assertThat(exception.getMessage(), is(equalTo("Invalid payload: [\nhello] was provided.")));
            assertThat(exception.getCause(), is(CoreMatchers.instanceOf(IOException.class)));
        }
    }

    @Test
    @DisplayName("Throws InvalidPayloadException if AvroRuntimeException encountered when deserialising a message")
    void testDeserialiseDataThrowsInvalidPayloadExceptionlIfAvroRuntimeExceptionEncountered() {
        // given
        try(ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser()) {

            // when
            Executable actual = () -> deserialiser.deserialize("topic", "invalid".getBytes(
                    StandardCharsets.UTF_8));

            // then
            InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
            assertThat(exception.getMessage(), is(equalTo("Invalid payload: [invalid] was provided.")));
            assertThat(exception.getCause(), is(CoreMatchers.instanceOf(AvroRuntimeException.class)));
        }
    }

}