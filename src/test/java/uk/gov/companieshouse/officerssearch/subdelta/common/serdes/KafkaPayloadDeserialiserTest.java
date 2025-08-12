package uk.gov.companieshouse.officerssearch.subdelta.common.serdes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.PREVIOUS_OFFICER_ID;

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
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.InvalidPayloadException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

class KafkaPayloadDeserialiserTest {

    @Test
    @DisplayName("Deserialise a ResourceChangedData serialised as Avro")
    void testDeserialiseResourceChangedData() throws IOException {
        try (KafkaPayloadDeserialiser<ResourceChangedData> deserialiser = new KafkaPayloadDeserialiser<>(
                ResourceChangedData.class)) {

            // given
            ResourceChangedData changeData = new ResourceChangedData("resource_kind",
                    "resource_uri", "context_id", "resource_id", "data",
                    new EventRecord("published_at", "event_type", Collections.emptyList()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
            writer.write(changeData, encoder);

            // when
            ResourceChangedData actual = deserialiser.deserialize("topic", outputStream.toByteArray());

            // then
            assertThat(actual, is(equalTo(changeData)));
        }
    }

    @Test
    @DisplayName("Deserialise a OfficerMerge serialised as Avro")
    void testDeserialiseOfficerMerge() throws IOException {
        try (KafkaPayloadDeserialiser<OfficerMerge> deserialiser = new KafkaPayloadDeserialiser<>(
                OfficerMerge.class)) {

            // given
            OfficerMerge officerMerge = new OfficerMerge(OFFICER_ID, PREVIOUS_OFFICER_ID, CONTEXT_ID);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<OfficerMerge> writer = new ReflectDatumWriter<>(OfficerMerge.class);
            writer.write(officerMerge, encoder);

            // when
            OfficerMerge actual = deserialiser.deserialize("topic", outputStream.toByteArray());

            // then
            assertThat(actual, is(equalTo(officerMerge)));
        }
    }

    @Test
    @DisplayName("Throws InvalidPayloadException if IOException encountered when deserialising a message")
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfIOExceptionEncountered()
            throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        try (KafkaPayloadDeserialiser<ResourceChangedData> deserialiser = new KafkaPayloadDeserialiser<>(
                ResourceChangedData.class)) {

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
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfAvroRuntimeExceptionEncountered() {
        // given
        try (KafkaPayloadDeserialiser<ResourceChangedData> deserialiser = new KafkaPayloadDeserialiser<>(
                ResourceChangedData.class)) {

            // when
            Executable actual = () -> deserialiser.deserialize("topic", "invalid".getBytes(StandardCharsets.UTF_8));

            // then
            InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
            assertThat(exception.getMessage(), is(equalTo("Invalid payload: [invalid] was provided.")));
            assertThat(exception.getCause(), is(CoreMatchers.instanceOf(AvroRuntimeException.class)));
        }
    }

}