package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.CONTEXT_ID;

import java.util.Collections;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public final class ResourceChangedTestUtils {

    public static final String STREAM_COMPANY_OFFICERS_TOPIC = "stream-company-officers";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC = "stream-company-officers-officers-search-consumer-retry";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC = "stream-company-officers-officers-search-consumer-error";
    public static final String RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC = "stream-company-officers-officers-search-consumer-invalid";
    public static final String APPOINTMENT_ID = "appointment_id";
    public static final String COMPANY_NUMBER = "company_number";

    private ResourceChangedTestUtils() {
    }

    public static final ResourceChangedData RESOURCE_CHANGED_MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "changed", Collections.emptyList()))
            .build();

    public static final ResourceChangedData RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD = ResourceChangedData.newBuilder()
            .setResourceKind("company-officers")
            .setResourceUri(String.format("/company/%s/appointments/%s", COMPANY_NUMBER, APPOINTMENT_ID))
            .setContextId(CONTEXT_ID)
            .setResourceId(APPOINTMENT_ID)
            .setData("{}")
            .setEvent(new EventRecord("", "deleted", Collections.emptyList()))
            .build();
}
