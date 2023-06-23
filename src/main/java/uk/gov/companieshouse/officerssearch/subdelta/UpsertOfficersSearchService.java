package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
class UpsertOfficersSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final OfficerAppointmentsClient officerAppointmentsClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final ObjectMapper objectMapper;

    public UpsertOfficersSearchService(OfficerAppointmentsClient officerAppointmentsClient,
            SearchApiClient searchApiClient, IdExtractor idExtractor, ObjectMapper objectMapper) {
        this.officerAppointmentsClient = officerAppointmentsClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.objectMapper = objectMapper;
    }

    public void processMessage(ResourceChangedData payload) {
        String logContext = payload.getContextId();
        OfficerSummary officer = getOfficerSummary(payload, logContext);
        String officerId = idExtractor.extractOfficerIdFromSelfLink(
                officer.getLinks().getSelf());

        officerAppointmentsClient.getOfficerAppointmentsList(
                        officer.getLinks().getOfficer().getSelf(), logContext)
                .ifPresent(
                        appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList, logContext));
    }

    private OfficerSummary getOfficerSummary(ResourceChangedData payload, String logContext) {
        try {
            return objectMapper.readValue(payload.getData(), OfficerSummary.class);
        } catch (JsonProcessingException e) {
            LOGGER.errorContext(logContext, "Unable to parse payload data", e, null);
            throw new NonRetryableException("Unable to parse payload data", e);
        }
    }
}
