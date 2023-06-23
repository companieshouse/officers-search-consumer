package uk.gov.companieshouse.officerssearch.subdelta;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MAIN_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.messagePayloadBytes;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.companieshouse.stream.ResourceChangedData;

@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(
        topics = {MAIN_TOPIC, RETRY_TOPIC, ERROR_TOPIC, INVALID_TOPIC},
        controlledShutdown = true,
        partitions = 1
)
@Import(TestConfig.class)
@ActiveProfiles("test_main_retryable")
class ConsumerRetryableExceptionTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private CountDownLatch latch;

    @MockBean
    private ServiceRouter router;

    @Captor
    private ArgumentCaptor<Message<ResourceChangedData>> messageArgumentCaptor;

    @Test
    void testRepublishToErrorTopicThroughRetryTopics() throws Exception {
        //given
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(RetryableException.class).when(router).route(any());

        //when
        testProducer.send(
                new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(), "key",
                        messagePayloadBytes(MESSAGE_PAYLOAD)));
        if (!latch.await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, MAIN_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, RETRY_TOPIC), is(4));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, ERROR_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, INVALID_TOPIC), is(0));
        verify(router, times(5)).route(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload(), is(MESSAGE_PAYLOAD));
    }
}
