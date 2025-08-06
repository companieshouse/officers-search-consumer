package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Service
public class ResourceChangedServiceRouter {

    private static final String EVENT_CHANGED = "changed";
    private static final String EVENT_DELETED = "deleted";

    private final ResourceChangedUpsertService resourceChangedUpsertService;
    private final ResourceChangedDeleteService resourceChangedDeleteService;

    ResourceChangedServiceRouter(ResourceChangedUpsertService resourceChangedUpsertService, ResourceChangedDeleteService resourceChangedDeleteService) {
        this.resourceChangedUpsertService = resourceChangedUpsertService;
        this.resourceChangedDeleteService = resourceChangedDeleteService;
    }

    public void route(Message<ResourceChangedData> message) {
        ResourceChangedData payload = message.getPayload();

        if (EVENT_CHANGED.equals(payload.getEvent().getType())) {
            resourceChangedUpsertService.processMessage(payload);
        } else if (EVENT_DELETED.equals(payload.getEvent().getType())) {
            resourceChangedDeleteService.processMessage(payload);
        } else {
            throw new NonRetryableException(
                    String.format("Unable to handle message with log context [%s]", payload.getContextId()));
        }
    }
}