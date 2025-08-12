package uk.gov.companieshouse.officerssearch.subdelta.officermerge;

import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_ID;

import uk.gov.companieshouse.officermerge.OfficerMerge;

public final class OfficerMergeTestUtils {

    public static final String OFFICER_MERGE_TOPIC = "officer-merge";
    public static final String OFFICER_MERGE_RETRY_TOPIC = "officer-merge-officers-search-consumer-retry";
    public static final String OFFICER_MERGE_ERROR_TOPIC = "officer-merge-officers-search-consumer-error";
    public static final String OFFICER_MERGE_INVALID_TOPIC = "officer-merge-officers-search-consumer-invalid";
    public static final String PREVIOUS_OFFICER_ID = "previous_officer_id";
    public static final String OFFICER_APPOINTMENTS_LINK_MERGE = "/officers/previous_officer_id/appointments";

    private OfficerMergeTestUtils() {
    }

    public static final OfficerMerge OFFICER_MERGE_MESSAGE_PAYLOAD = OfficerMerge.newBuilder()
            .setOfficerId(OFFICER_ID)
            .setContextId(CONTEXT_ID)
            .setPreviousOfficerId(PREVIOUS_OFFICER_ID)
            .build();
}
