package uk.gov.companieshouse.officerssearch.subdelta.officermerge;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeRouter;

public class OfficerMergeConsumer {

    private final OfficerMergeRouter router;
    private final MessageFlags messageFlags;

    public OfficerMergeConsumer(OfficerMergeRouter router, MessageFlags messageFlags) {
        this.router = router;
        this.messageFlags = messageFlags;
    }

    @KafkaListener(
            id = "${consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = "${officer-merge.consumer.topic}",
            groupId = "${consumer.group-id}"
    )
    public void consume(Message<OfficerMerge> message) {
        try {
            router.route(message);
        } catch (RetryableException e) {
            messageFlags.setRetryable(true);
            throw e;
        }
    }
}
