package uk.gov.companieshouse.officerssearch.subdelta.search;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class UpsertService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;
    private final IdExtractor idExtractor;
    private final OfficerDeserialiser deserialiser;

    UpsertService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient,
            IdExtractor idExtractor, OfficerDeserialiser deserialiser) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
        this.idExtractor = idExtractor;
        this.deserialiser = deserialiser;
    }

    @Override
    public void processMessage(ResourceChangedData payload) {
        String officerAppointmentsLink = deserialiser.deserialiseOfficerData(payload.getData())
                .getLinks()
                .getOfficer()
                .getAppointments();

        String officerId = idExtractor.extractOfficerId(officerAppointmentsLink);
        DataMapHolder.get().officerId(officerId);

        appointmentsApiClient.getOfficerAppointmentsList(officerAppointmentsLink)
                .ifPresentOrElse(appointmentList -> searchApiClient.upsertOfficerAppointments(officerId,
                                appointmentList),
                        () -> {
                            LOGGER.error("Officer appointments unavailable.", DataMapHolder.getLogMap());
                            throw new NonRetryableException("Officer appointments unavailable");
                        });
    }
}
