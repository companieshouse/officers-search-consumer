package uk.gov.companieshouse.officerssearch.subdelta.search;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.officerssearch.subdelta.kafka.OfficerMergeData;

@Component
public class OfficerMergeService {

    private static final String URI = "/officers/%s/appointments";

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;

    public OfficerMergeService(AppointmentsApiClient appointmentsApiClient,
            SearchApiClient searchApiClient) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
    }

    public void process(String payload) {
        final String previousOfficerId = payload;
        appointmentsApiClient.getOfficerAppointmentsListForGet(URI.formatted(previousOfficerId))
                .ifPresentOrElse(appointmentList -> {
                    // PUT Primary Search API
                    searchApiClient.upsertOfficerAppointments(previousOfficerId, appointmentList);
                }, () -> {
                    // DELETE Primary Search API
                    searchApiClient.deleteOfficerAppointments(previousOfficerId);
                });
    }
}
