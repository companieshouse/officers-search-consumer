package uk.gov.companieshouse.officerssearch.subdelta;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public final class TestUtils {

    public static final String MAIN_TOPIC = "echo";
    public static final String RETRY_TOPIC = "echo-echo-consumer-retry";
    public static final String ERROR_TOPIC = "echo-echo-consumer-error";
    public static final String INVALID_TOPIC = "echo-echo-consumer-invalid";
    public static final String CONTEXT_ID = "context_id";
    public static final String COMPANY_NUMBER = "company_number";
    public static final String APPOINTMENT_ID = "appointment_id";
    public static final String OFFICER_ID = "officer_id";
    public static final String OFFICER_APPOINTMENTS_LINK = "/officers/abc123def456ghi789/appointments";
    public static final String COMPANY_APPOINTMENT_LINK = "/company/12345678/appointments/987ihg654fed321cba";
    public static final Map<String, String> TEST_INTERNAL_GET_PARAMS = Map.of("items_per_page", "500");

    public static final ResourceChangedData MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(
                    String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "changed", Collections.emptyList()))
            .build();

    public static final ResourceChangedData DELETED_MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(
                    String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "deleted", Collections.emptyList()))
            .build();

    private TestUtils() {
    }

    public static int noOfRecordsForTopic(ConsumerRecords<?, ?> records, String topic) {
        int count = 0;
        for (ConsumerRecord<?, ?> ignored : records.records(topic)) {
            count++;
        }
        return count;
    }

    public static byte[] messagePayloadBytes(ResourceChangedData data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                    ResourceChangedData.class);
            writer.write(data, encoder);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
