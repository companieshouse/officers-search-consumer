package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
class ServiceRouter {

    private static final String EVENT_CHANGED = "changed";

    private final UpsertOfficersSearchService upsertOffersSearchService;

    ServiceRouter(UpsertOfficersSearchService upsertOffersSearchService) {
        this.upsertOffersSearchService = upsertOffersSearchService;
    }

    public void route(Message<ResourceChangedData> message) {
        ResourceChangedData payload = message.getPayload();
        String logContext = payload.getContextId();

        if (payload.getEvent().getType().equals(EVENT_CHANGED)) {
            upsertOffersSearchService.processMessage(payload);
        } else {
            throw new NonRetryableException(
                    String.format("Unable to handle message with log context [%s]", logContext));
        }
    }
}