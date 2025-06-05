package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.search.ServiceRouter;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.INTEGRATION;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.kafka.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag(INTEGRATION)
class ConsumerNonRetryableExceptionTest extends AbstractKafkaTest {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @MockitoBean
    private ServiceRouter router;

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
    void testRepublishToInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                ResourceChangedData.class);
        writer.write(new ResourceChangedData("", "", "", "", "{}",
                new EventRecord("", "", Collections.emptyList())), encoder);

        doThrow(NonRetryableException.class).when(router).route(any());

        //when
        testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(),
                        "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                OFFICERS_SEARCH_CONSUMER_RETRY_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                OFFICERS_SEARCH_CONSUMER_ERROR_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                OFFICERS_SEARCH_CONSUMER_INVALID_TOPIC), is(1));
        verify(router).route(any());
    }
}
