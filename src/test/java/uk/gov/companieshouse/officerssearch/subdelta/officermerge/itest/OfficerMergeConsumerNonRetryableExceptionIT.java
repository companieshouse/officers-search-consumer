package uk.gov.companieshouse.officerssearch.subdelta.officermerge.itest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.officermerge.OfficerMergeTestUtils.OFFICER_MERGE_TOPIC;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
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
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeService;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OfficerMergeConsumerNonRetryableExceptionIT extends AbstractKafkaTest {

    @MockitoBean
    private OfficerMergeService router;

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Override
    public List<String> getSubscribedTopics() {
        return List.of(OFFICER_MERGE_TOPIC, OFFICER_MERGE_RETRY_TOPIC,
                OFFICER_MERGE_ERROR_TOPIC, OFFICER_MERGE_INVALID_TOPIC);
    }

    @Test
    void testRepublishToInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                ResourceChangedData.class);
        writer.write(new ResourceChangedData("", "", "", "", "{}",
                new EventRecord("", "", Collections.emptyList())), encoder);

        doThrow(NonRetryableException.class).when(router).processMessage(any());

        //when
        testProducer.send(
                new ProducerRecord<>(OFFICER_MERGE_TOPIC, 0, System.currentTimeMillis(),
                        "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);

        //then
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_TOPIC)).isOne();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, OFFICER_MERGE_INVALID_TOPIC)).isOne();
        verify(router).processMessage(any());
    }

    @Test
    void testPublishToInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("bad data", encoder);

        //when
        Future<RecordMetadata> future = testProducer.send(
                new ProducerRecord<>(OFFICER_MERGE_TOPIC, 0, System.currentTimeMillis(), "key",
                        outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);

        //then
        assertThat(recordsPerTopic(records, OFFICER_MERGE_TOPIC)).isOne();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(records, OFFICER_MERGE_INVALID_TOPIC)).isOne();
        verifyNoInteractions(router);
    }
}
