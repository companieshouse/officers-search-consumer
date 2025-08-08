package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service.ResourceChangedServiceRouter;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * Consumes messages from the configured main Kafka topic.
 */
@Component
public class ResourceChangedConsumer {

    private final ResourceChangedServiceRouter router;
    private final MessageFlags messageFlags;

    public ResourceChangedConsumer(ResourceChangedServiceRouter router, MessageFlags messageFlags) {
        this.router = router;
        this.messageFlags = messageFlags;
    }

    /**
     * Consume a resource-changed message from Kafka.
     *
     * @param message A message containing a payload.
     */
    @KafkaListener(
            id = "${resource-changed.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = "${resource-changed.consumer.topic}",
            groupId = "${resource-changed.consumer.group-id}"
    )
    public void consume(Message<ResourceChangedData> message) {
        try {
            router.route(message);
        } catch (RetryableException e) {
            messageFlags.setRetryable(true);
            throw e;
        }
    }
}
