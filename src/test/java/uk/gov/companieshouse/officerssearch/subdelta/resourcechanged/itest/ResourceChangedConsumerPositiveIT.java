package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.itest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.messagePayloadBytes;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service.ResourceChangedServiceRouter;
import uk.gov.companieshouse.stream.ResourceChangedData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceChangedConsumerPositiveIT extends AbstractKafkaTest {

    @MockitoBean
    private ResourceChangedServiceRouter router;

    @Captor
    private ArgumentCaptor<Message<ResourceChangedData>> messageArgumentCaptor;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Override
    List<String> getSubscribedTopics() {
        return List.of(STREAM_COMPANY_OFFICERS_TOPIC, OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC,
                OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC, OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC);
    }

    @Test
    void testConsumeFromMainTopic() throws Exception {
        //given

        //when
        testProducer.send(new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(), "key",
                messagePayloadBytes(MESSAGE_PAYLOAD)));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 1);
        assertThat(recordsPerTopic(records, STREAM_COMPANY_OFFICERS_TOPIC), is(1));
        assertThat(recordsPerTopic(records, OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC), is(0));
        assertThat(recordsPerTopic(records, OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC), is(0));
        assertThat(recordsPerTopic(records, OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC), is(0));
        verify(router).route(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload(), is(MESSAGE_PAYLOAD));
    }
}
