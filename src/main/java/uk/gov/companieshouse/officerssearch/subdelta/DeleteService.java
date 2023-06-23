package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
class DeleteService implements Service {

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;

    DeleteService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
            IdExtractor idExtractor, OfficerDeserialiser deserialiser) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
    }

    @Override
    public void processMessage(ResourceChangedData changedData, String contextId) {
        appointmentsApiClient.getAppointment(changedData.getResourceUri(), contextId)
                .ifPresent(officerSummary -> {
                    throw new RetryableException("Appointment has not yet been deleted");
                });

        OfficerSummary officer = deserialiser.deserialiseOfficerData(changedData.getData(), contextId);

        String officerId = idExtractor.extractOfficerIdFromSelfLink(officer
                .getLinks()
                .getOfficer()
                .getSelf());

        appointmentsApiClient.getOfficerAppointmentsList(officerId, contextId)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList, contextId),
                        () -> searchApiClient.deleteOfficerAppointments(officerId, contextId));
    }
}
