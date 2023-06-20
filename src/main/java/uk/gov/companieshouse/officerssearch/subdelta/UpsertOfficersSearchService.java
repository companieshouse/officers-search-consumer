package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.stereotype.Service;

@Service
class UpsertOfficersSearchService {

    private final OfficerAppointmentsClient officerAppointmentsClient;
    private final SearchApiClient searchApiClient;

    public UpsertOfficersSearchService(OfficerAppointmentsClient officerAppointmentsClient,
            SearchApiClient searchApiClient) {
        this.officerAppointmentsClient = officerAppointmentsClient;
        this.searchApiClient = searchApiClient;
    }

    public void processMessage(String resourceUri, String officerId, String logContext) {
        officerAppointmentsClient.getOfficerAppointmentsList(resourceUri, logContext)
                .ifPresent(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                        appointmentList, logContext));
    }
}
