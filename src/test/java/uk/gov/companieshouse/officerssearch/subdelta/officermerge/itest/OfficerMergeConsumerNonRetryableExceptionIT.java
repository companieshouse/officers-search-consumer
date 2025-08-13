package uk.gov.companieshouse.officerssearch.subdelta.officermerge.itest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.writePayloadToBytes;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_TOPIC;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.itest.AbstractKafkaTest;
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OfficerMergeConsumerNonRetryableExceptionIT extends AbstractKafkaTest {

    @MockitoBean
    private OfficerMergeService service;

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Override
    public List<String> getSubscribedTopics() {
        return List.of(OFFICER_MERGE_TOPIC, OFFICER_MERGE_RETRY_TOPIC, OFFICER_MERGE_ERROR_TOPIC, OFFICER_MERGE_INVALID_TOPIC);
    }

    @Test
    void testRepublishToInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        doThrow(NonRetryableException.class).when(service).processMessage(any());

        //when
        testProducer.send(new ProducerRecord<>(OFFICER_MERGE_TOPIC, 0, System.currentTimeMillis(), "key",
                writePayloadToBytes(OFFICER_MERGE_MESSAGE_PAYLOAD, OfficerMerge.class)));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_TOPIC)).isOne();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_INVALID_TOPIC)).isOne();
        verify(service).processMessage(any());
    }

    @Test
    void testPublishToInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        //given

        //when
        Future<RecordMetadata> future = testProducer.send(
                new ProducerRecord<>(OFFICER_MERGE_TOPIC, 0, System.currentTimeMillis(), "key",
                        writePayloadToBytes("bad data", String.class)));
        future.get();

        //then
        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(recordsPerTopic(records, OFFICER_MERGE_TOPIC)).isOne();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_INVALID_TOPIC)).isOne();
        verifyNoInteractions(service);
    }
}
