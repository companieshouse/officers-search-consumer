package uk.gov.companieshouse.officerssearch.subdelta.common;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public final class TestUtils {

    public static final String STREAM_COMPANY_OFFICERS_TOPIC = "stream-company-officers";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC = "stream-company-officers-officers-search-consumer-retry";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC = "stream-company-officers-officers-search-consumer-error";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC = "stream-company-officers-officers-search-consumer-invalid";
    public static final String OFFICER_MERGE_TOPIC = "officer-merge";
    public static final String OFFICER_MERGE_RETRY_TOPIC = "officer-merge-officers-search-consumer-retry";
    public static final String OFFICER_MERGE_ERROR_TOPIC = "officer-merge-officers-search-consumer-error";
    public static final String OFFICER_MERGE_INVALID_TOPIC = "officer-merge-officers-search-consumer-invalid";
    public static final String CONTEXT_ID = "context_id";
    public static final String COMPANY_NUMBER = "company_number";
    public static final String APPOINTMENT_ID = "appointment_id";
    public static final String OFFICER_ID = "officer_id";
    public static final String PREVIOUS_OFFICER_ID = "previous_officer_id";
    public static final String GET_APPOINTMENT_CALL = "Appointments API GET Appointment";
    public static final String GET_OFFICER_APPOINTMENTS_CALL = "Appointments API GET Officer Appointments";
    public static final String SEARCH_API_PUT = "Officer Search API PUT";
    public static final String SEARCH_API_DELETE = "Officer Search API DELETE";
    public static final String OFFICERS_SEARCH_LINK = "/officers-search/officers/" + OFFICER_ID;
    public static final String OFFICER_APPOINTMENTS_LINK = "/officers/abc123def456ghi789/appointments";
    public static final String OFFICER_APPOINTMENTS_LINK_MERGE = "/officers/previous_officer_id/appointments";
    public static final String COMPANY_APPOINTMENT_LINK = "/company/12345678/appointments/987ihg654fed321cba";

    public static final ResourceChangedData RESOURCE_CHANGED_MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(
                    String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "changed", Collections.emptyList()))
            .build();

    public static final ResourceChangedData RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(
                    String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "deleted", Collections.emptyList()))
            .build();

    public static final OfficerMerge OFFICER_MERGE_MESSAGE_PAYLOAD = OfficerMerge.newBuilder()
            .setOfficerId(OFFICER_ID)
            .setContextId(CONTEXT_ID)
            .setPreviousOfficerId(PREVIOUS_OFFICER_ID)
            .build();

    private TestUtils() {
    }

    public static <T> byte[] writePayloadToBytes(T data, Class<T> type) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<T> writer = new ReflectDatumWriter<>(type);
            writer.write(data, encoder);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
