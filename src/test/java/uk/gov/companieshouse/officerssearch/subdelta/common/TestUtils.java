package uk.gov.companieshouse.officerssearch.subdelta.common;

import java.io.ByteArrayOutputStream;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;

public final class TestUtils {

    public static final String CONTEXT_ID = "context_id";
    public static final String OFFICER_ID = "officer_id";
    public static final String GET_APPOINTMENT_CALL = "Appointments API GET Appointment";
    public static final String GET_OFFICER_APPOINTMENTS_CALL = "Appointments API GET Officer Appointments";
    public static final String SEARCH_API_PUT = "Officer Search API PUT";
    public static final String SEARCH_API_DELETE = "Officer Search API DELETE";
    public static final String OFFICERS_SEARCH_LINK = "/officers-search/officers/" + OFFICER_ID;
    public static final String OFFICER_APPOINTMENTS_LINK = "/officers/abc123def456ghi789/appointments";
    public static final String COMPANY_APPOINTMENT_LINK = "/company/12345678/appointments/987ihg654fed321cba";

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
