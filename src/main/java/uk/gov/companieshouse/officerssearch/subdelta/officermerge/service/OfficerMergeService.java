package uk.gov.companieshouse.officerssearch.subdelta.officermerge.service;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.AppointmentsApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.SearchApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class OfficerMergeService implements MergeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String URI = "/officers/%s/appointments";

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;

    public OfficerMergeService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
    }

    @Override
    public void processMessage(Message<OfficerMerge> message) {
        OfficerMerge officerMerge = message.getPayload();
        final String previousOfficerId = officerMerge.getPreviousOfficerId();
        DataMapHolder.get()
                .officerId(officerMerge.getOfficerId())
                .previousOfficerId(previousOfficerId);
        appointmentsApiClient.getOfficerAppointmentsListForDelete(URI.formatted(previousOfficerId))
                .ifPresentOrElse(appointmentList -> {
                    LOGGER.info("Updating previous officer in index", DataMapHolder.getLogMap());
                    searchApiClient.upsertOfficerAppointments(previousOfficerId, appointmentList);
                }, () -> {
                    LOGGER.info("Deleting previous officer from index", DataMapHolder.getLogMap());
                    searchApiClient.deleteOfficerAppointments(previousOfficerId);
                });
    }
}
