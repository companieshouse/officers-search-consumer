package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.request.QueryParam;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.List;

import static uk.gov.companieshouse.officerssearch.subdelta.QueryParamBuilder.INTERNAL_GET_PARAMS;

@Component
class DeleteService implements Service {

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;
    private final QueryParamBuilder queryParamBuilder;

    DeleteService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
                  IdExtractor idExtractor, OfficerDeserialiser deserialiser, QueryParamBuilder queryParamBuilder) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
        this.queryParamBuilder = queryParamBuilder;
    }

    @Override
    public void processMessage(ResourceChangedData changedData) {
        String contextId = changedData.getContextId();
        appointmentsApiClient.getAppointment(changedData.getResourceUri(), contextId)
                .ifPresent(officerSummary -> {
                    throw new RetryableException("Appointment has not yet been deleted");
                });

        String officerAppointmentsLink = deserialiser.deserialiseOfficerData(changedData.getData(), contextId)
                .getLinks()
                .getOfficer()
                .getAppointments();

        String officerId = idExtractor.extractOfficerId(officerAppointmentsLink);
        List<QueryParam> queryParams = queryParamBuilder.build(INTERNAL_GET_PARAMS);

        appointmentsApiClient.getOfficerAppointmentsList(officerAppointmentsLink, contextId, queryParams)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList, contextId),
                        () -> searchApiClient.deleteOfficerAppointments(officerId, contextId));
    }
}
