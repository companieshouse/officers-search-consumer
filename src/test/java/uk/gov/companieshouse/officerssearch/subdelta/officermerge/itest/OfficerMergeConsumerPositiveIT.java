package uk.gov.companieshouse.officerssearch.subdelta.officermerge.itest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_MERGE_ERROR_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_MERGE_INVALID_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_MERGE_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_MERGE_RETRY_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_MERGE_TOPIC;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.RESOURCE_CHANGED_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.writeOfficerMergePayloadToBytes;

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
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.itest.AbstractKafkaTest;
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeRouter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OfficerMergeConsumerPositiveIT extends AbstractKafkaTest {

    @MockitoBean
    private OfficerMergeRouter router;

    @Captor
    private ArgumentCaptor<Message<OfficerMerge>> messageArgumentCaptor;

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Override
    public List<String> getSubscribedTopics() {
        return List.of(OFFICER_MERGE_TOPIC, OFFICER_MERGE_RETRY_TOPIC, OFFICER_MERGE_ERROR_TOPIC, OFFICER_MERGE_INVALID_TOPIC);
    }

    @Test
    void testConsumeFromMainTopic() throws Exception {
        //given

        //when
        testProducer.send(new ProducerRecord<>(OFFICER_MERGE_TOPIC, 0, System.currentTimeMillis(), "key",
                writeOfficerMergePayloadToBytes(OFFICER_MERGE_MESSAGE_PAYLOAD)));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 1);
        assertThat(recordsPerTopic(records, OFFICER_MERGE_TOPIC), is(1));
        assertThat(recordsPerTopic(records, OFFICER_MERGE_RETRY_TOPIC), is(0));
        assertThat(recordsPerTopic(records, OFFICER_MERGE_ERROR_TOPIC), is(0));
        assertThat(recordsPerTopic(records, OFFICER_MERGE_INVALID_TOPIC), is(0));
        verify(router).route(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload(), is(RESOURCE_CHANGED_MESSAGE_PAYLOAD));
    }
}
