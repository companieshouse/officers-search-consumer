package uk.gov.companieshouse.officerssearch.subdelta.officermerge;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeService;

@Component
public class OfficerMergeConsumer {

    private final OfficerMergeService router;
    private final MessageFlags messageFlags;

    public OfficerMergeConsumer(OfficerMergeService router, MessageFlags messageFlags) {
        this.router = router;
        this.messageFlags = messageFlags;
    }

    @KafkaListener(
            id = "${officer-merge.consumer.topic}-consumer",
            containerFactory = "officerMergeKafkaListenerContainerFactory",
            topics = "${officer-merge.consumer.topic}",
            groupId = "${consumer.group-id}"
    )
    public void consume(Message<OfficerMerge> message) {
        try {
            router.processMessage(message);
        } catch (RetryableException e) {
            messageFlags.setRetryable(true);
            throw e;
        }
    }
}
