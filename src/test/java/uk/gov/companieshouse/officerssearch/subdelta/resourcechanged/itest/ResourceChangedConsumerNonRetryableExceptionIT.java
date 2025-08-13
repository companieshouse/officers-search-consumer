package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.itest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.writePayloadToBytes;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.STREAM_COMPANY_OFFICERS_TOPIC;

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
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.itest.AbstractKafkaTest;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service.ResourceChangedServiceRouter;
import uk.gov.companieshouse.stream.ResourceChangedData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceChangedConsumerNonRetryableExceptionIT extends AbstractKafkaTest {

    @MockitoBean
    private ResourceChangedServiceRouter router;

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Override
    public List<String> getSubscribedTopics() {
        return List.of(STREAM_COMPANY_OFFICERS_TOPIC, RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC,
                RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC, RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC);
    }

    @Test
    void testRepublishToInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        doThrow(NonRetryableException.class).when(router).route(any());

        //when
        testProducer.send(new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(), "key",
                writePayloadToBytes(RESOURCE_CHANGED_MESSAGE_PAYLOAD, ResourceChangedData.class)));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(recordsPerTopic(consumerRecords, STREAM_COMPANY_OFFICERS_TOPIC)).isOne();
        assertThat(recordsPerTopic(consumerRecords, RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC)).isOne();
        verify(router).route(any());
    }

    @Test
    void testPublishToInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        //given

        //when
        Future<RecordMetadata> future = testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(), "key",
                        writePayloadToBytes("bad data", String.class)));
        future.get();

        //then
        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(recordsPerTopic(records, STREAM_COMPANY_OFFICERS_TOPIC)).isOne();
        assertThat(recordsPerTopic(records, RESOURCE_CHANGED_COMPANY_OFFICERS_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, RESOURCE_CHANGED_COMPANY_OFFICERS_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, RESOURCE_CHANGED_COMPANY_OFFICERS_INVALID_TOPIC)).isOne();
        verifyNoInteractions(router);
    }
}
