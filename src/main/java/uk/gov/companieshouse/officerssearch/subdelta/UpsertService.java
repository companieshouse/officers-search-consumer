package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;
import static uk.gov.companieshouse.officerssearch.subdelta.QueryParamBuilder.INTERNAL_GET_PARAMS;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.request.QueryParam;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.List;

@Component
class UpsertService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;
    private final QueryParamBuilder queryParamBuilder;

    UpsertService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
                  IdExtractor idExtractor, OfficerDeserialiser deserialiser, QueryParamBuilder queryParamBuilder) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
        this.queryParamBuilder = queryParamBuilder;
    }

    @Override
    public void processMessage(ResourceChangedData payload) {
        String logContext = payload.getContextId();
        String officerAppointmentsLink = deserialiser.deserialiseOfficerData(payload.getData(), logContext)
                .getLinks()
                .getOfficer()
                .getAppointments();
        List<QueryParam> queryParams = queryParamBuilder.build(INTERNAL_GET_PARAMS);

        appointmentsApiClient.getOfficerAppointmentsList(officerAppointmentsLink, logContext, queryParams)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(
                                idExtractor.extractOfficerId(officerAppointmentsLink),
                                appointmentList,
                                logContext),
                        () -> {
                            LOGGER.error("Officer appointments unavailable, contextId: " + logContext);
                            throw new NonRetryableException("Officer appointments unavailable");
                        });
    }
}
