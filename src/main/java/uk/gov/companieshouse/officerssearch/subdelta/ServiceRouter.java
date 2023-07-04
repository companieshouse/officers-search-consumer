package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
class ServiceRouter {

    private static final String EVENT_CHANGED = "changed";
    private static final String EVENT_DELETED = "deleted";

    private final UpsertService upsertService;
    private final DeleteService deleteService;

    ServiceRouter(UpsertService upsertService, DeleteService deleteService) {
        this.upsertService = upsertService;
        this.deleteService = deleteService;
    }

    public void route(Message<ResourceChangedData> message) {
        ResourceChangedData payload = message.getPayload();

        if (EVENT_CHANGED.equals(payload.getEvent().getType())) {
            upsertService.processMessage(payload);
        } else if (EVENT_DELETED.equals(payload.getEvent().getType())) {
            deleteService.processMessage(payload);
        } else {
            throw new NonRetryableException(
                    String.format("Unable to handle message with log context [%s]", payload.getContextId()));
        }
    }
}