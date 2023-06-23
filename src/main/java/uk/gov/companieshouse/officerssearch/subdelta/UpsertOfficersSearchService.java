package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
class UpsertOfficersSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;

    public UpsertOfficersSearchService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
            IdExtractor idExtractor, OfficerDeserialiser deserialiser) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
    }

    public void processMessage(ResourceChangedData payload) {
        String logContext = payload.getContextId();
        OfficerSummary officer = deserialiser.deserialiseOfficerData(payload.getData(), logContext);
        String officerId = idExtractor.extractOfficerIdFromSelfLink(officer.getLinks().getSelf());

        appointmentsApiClient.getOfficerAppointmentsList(officer.getLinks().getOfficer().getSelf(), logContext)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList, logContext),
                        () -> {
                            LOGGER.error("Officer appointments unavailable, contextId: " + logContext);
                            throw new NonRetryableException("Officer appointments unavailable");
                        });
    }
}
