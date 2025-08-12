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
public class OfficerMergeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String URI = "/officers/%s/appointments";
    private static final String APPOINTMENTS_FOUND_MESSAGE = "Appointments found for merge, upserting officer appointments";
    private static final String APPOINTMENTS_NOT_FOUND_MESSAGE = "Appointments not found for merge, deleting officer appointments";

    private final AppointmentsApiClient appointmentsApiClient;
    private final SearchApiClient searchApiClient;

    public OfficerMergeService(AppointmentsApiClient appointmentsApiClient, SearchApiClient searchApiClient) {
        this.appointmentsApiClient = appointmentsApiClient;
        this.searchApiClient = searchApiClient;
    }

    public void processMessage(Message<OfficerMerge> message) {
        final String previousOfficerId =  message.getPayload().getPreviousOfficerId();
        appointmentsApiClient.getOfficerAppointmentsListForMerge(URI.formatted(previousOfficerId))
                .ifPresentOrElse(appointmentList -> {
                    LOGGER.info(APPOINTMENTS_FOUND_MESSAGE, DataMapHolder.getLogMap());
                    searchApiClient.upsertOfficerAppointments(previousOfficerId, appointmentList);
                }, () -> {
                    LOGGER.info(APPOINTMENTS_NOT_FOUND_MESSAGE, DataMapHolder.getLogMap());
                    searchApiClient.deleteOfficerAppointments(previousOfficerId);
                });
    }
}
