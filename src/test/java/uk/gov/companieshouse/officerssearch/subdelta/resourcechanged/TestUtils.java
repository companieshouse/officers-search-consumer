package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public final class TestUtils {

    public static final String STREAM_COMPANY_OFFICERS_TOPIC = "stream-company-officers";
    public static final String OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC = "stream-company-officers-officers-search-consumer-retry";
    public static final String OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC = "stream-company-officers-officers-search-consumer-error";
    public static final String OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC = "stream-company-officers-officers-search-consumer-invalid";
    public static final String CONTEXT_ID = "context_id";
    public static final String COMPANY_NUMBER = "company_number";
    public static final String APPOINTMENT_ID = "appointment_id";
    public static final String OFFICER_ID = "officer_id";
    public static final String GET_APPOINTMENT_CALL = "Appointments API GET Appointment";
    public static final String GET_OFFICER_APPOINTMENTS_CALL = "Appointments API GET Officer Appointments";
    public static final String SEARCH_API_PUT = "Officer Search API PUT";
    public static final String SEARCH_API_DELETE = "Officer Search API DELETE";
    public static final String OFFICERS_SEARCH_LINK = "/officers-search/officers/" + OFFICER_ID;
    public static final String OFFICER_APPOINTMENTS_LINK = "/officers/abc123def456ghi789/appointments";
    public static final String COMPANY_APPOINTMENT_LINK = "/company/12345678/appointments/987ihg654fed321cba";
    public static final String INTEGRATION = "integration-test";

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

    public static byte[] messagePayloadBytes(ResourceChangedData data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
            writer.write(data, encoder);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
