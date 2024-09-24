package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.messagePayloadBytes;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.officerssearch.subdelta.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.search.ServiceRouter;
import uk.gov.companieshouse.stream.ResourceChangedData;

@SpringBootTest
@WireMockTest(httpPort = 8888)
class ConsumerRetryableExceptionTest extends AbstractKafkaTest {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @MockBean
    private ServiceRouter router;

    @Captor
    private ArgumentCaptor<Message<ResourceChangedData>> messageArgumentCaptor;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testRepublishToErrorTopicThroughRetryTopics() throws Exception {
        //given
        doThrow(RetryableException.class).when(router).route(any());

        //when
        testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(), "key",
                        messagePayloadBytes(MESSAGE_PAYLOAD)));
        if (!testConsumerAspect.getLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC), is(4));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC), is(0));
        verify(router, times(5)).route(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload(), is(MESSAGE_PAYLOAD));
    }
}
