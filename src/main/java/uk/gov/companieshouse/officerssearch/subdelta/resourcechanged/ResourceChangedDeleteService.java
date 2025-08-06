package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.searchclient.AppointmentsApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes.OfficerDeserialiser;
import uk.gov.companieshouse.officerssearch.subdelta.common.searchclient.SearchApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDeleteService implements ResourceChangedService {

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;

    ResourceChangedDeleteService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
            IdExtractor idExtractor, OfficerDeserialiser deserialiser) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
    }

    @Override
    public void processMessage(ResourceChangedData changedData) {
        appointmentsApiClient.getAppointment(changedData.getResourceUri())
                .ifPresent(officerSummary -> {
                    throw new RetryableException("Appointment has not yet been deleted");
                });

        String officerAppointmentsLink = deserialiser.deserialiseOfficerData(changedData.getData())
                .getLinks()
                .getOfficer()
                .getAppointments();

        String officerId = idExtractor.extractOfficerId(officerAppointmentsLink);
        DataMapHolder.get().officerId(officerId);

        appointmentsApiClient.getOfficerAppointmentsListForDelete(officerAppointmentsLink)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList),
                        () -> searchApiClient.deleteOfficerAppointments(officerId));
    }
}
