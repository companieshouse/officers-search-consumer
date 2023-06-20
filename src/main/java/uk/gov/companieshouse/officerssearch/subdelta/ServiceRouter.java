package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
class ServiceRouter {

    private static final String EVENT_CHANGED = "changed";

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.NAMESPACE);

    private final UpsertOfficersSearchService upsertOffersSearchService;
    private final OfficerAppointmentsClient officerAppointmentsClient;

    ServiceRouter(UpsertOfficersSearchService upsertOffersSearchService,
            OfficerAppointmentsClient officerAppointmentsClient) {
        this.upsertOffersSearchService = upsertOffersSearchService;
        this.officerAppointmentsClient = officerAppointmentsClient;
    }

    public void route(Message<ResourceChangedData> message) {
        ResourceChangedData payload = message.getPayload();
        String logContext = payload.getContextId();

        officerAppointmentsClient.getOfficerId(payload)
                .ifPresent(officerId -> {

                    if (payload.getEvent().getType().equals(EVENT_CHANGED)) {
                        upsertOffersSearchService.processMessage(payload.getResourceUri(),
                                officerId, logContext);
                        LOGGER.infoContext(
                                logContext,
                                String.format("Upsert search for officer with id [%s]", officerId),
                                null);
                    } else {
                        throw new NonRetryableException(
                                String.format("Unable to handle message with log context [%s]",
                                        logContext));
                    }
                });
    }
}