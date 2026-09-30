package uk.gov.companieshouse.officerssearch.subdelta.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_MESSAGE;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_TOPIC;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvalidMessageRouterTest {

    private static final String SOURCE_TOPIC = "stream-officers";
    private static final String INVALID_TOPIC = "stream-officers-invalid";
    private static final String KEY = "some-key";
    private static final String VALUE = "some-value";

    @Mock
    private MessageFlags messageFlags;

    private InvalidMessageRouter router;

    @BeforeEach
    void setUp() {
        router = new InvalidMessageRouter();
        router.configure(Map.of(
                "message-flags", messageFlags,
                "invalid-message-topic", INVALID_TOPIC));
    }

    // ------------------------------------------------------------------
    // onSend - retryable path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Retryable: the record is passed through unchanged")
    void onSendReturnsRecordUnchangedWhenRetryable() {
        when(messageFlags.isRetryable()).thenReturn(true);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);

        ProducerRecord<String, Object> result = router.onSend(producerRecord);

        assertThat(result).isSameAs(producerRecord);
    }

    @Test
    @DisplayName("Retryable: the message flag is cleared so it doesn't leak into the next message on this thread")
    void onSendDestroysFlagWhenRetryable() {
        when(messageFlags.isRetryable()).thenReturn(true);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);

        router.onSend(producerRecord);

        verify(messageFlags).destroy();
    }

    // ------------------------------------------------------------------
    // onSend - non-retryable (invalid message) path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Non-retryable: the record is rerouted to the invalid topic, keeping the key and value")
    void onSendReroutesToInvalidTopicWhenNotRetryable() {
        when(messageFlags.isRetryable()).thenReturn(false);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);

        ProducerRecord<String, Object> result = router.onSend(producerRecord);

        assertThat(result.topic()).isEqualTo(INVALID_TOPIC);
        assertThat(result.key()).isEqualTo(KEY);
        assertThat(result.value()).isEqualTo(VALUE);
    }

    @Test
    @DisplayName("Non-retryable: the flag is not cleared, since there was nothing retryable to clear")
    void onSendDoesNotDestroyFlagWhenNotRetryable() {
        when(messageFlags.isRetryable()).thenReturn(false);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);

        router.onSend(producerRecord);

        verify(messageFlags, never()).destroy();
    }

    @Test
    @DisplayName("Non-retryable: original-topic/partition/offset/exception headers are read when present")
    void onSendReadsOriginalHeadersWhenPresent() {
        when(messageFlags.isRetryable()).thenReturn(false);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);
        producerRecord.headers().add(new RecordHeader(ORIGINAL_TOPIC, "original-topic".getBytes(StandardCharsets.UTF_8)));
        producerRecord.headers().add(new RecordHeader(ORIGINAL_PARTITION, BigInteger.valueOf(2).toByteArray()));
        producerRecord.headers().add(new RecordHeader(ORIGINAL_OFFSET, BigInteger.valueOf(42).toByteArray()));
        producerRecord.headers().add(new RecordHeader(EXCEPTION_MESSAGE, "boom".getBytes(StandardCharsets.UTF_8)));

        // Reading the headers only feeds a log line, so the externally observable
        // contract is that a fully-populated header set doesn't stop the reroute happening.
        ProducerRecord<String, Object> result = router.onSend(producerRecord);

        assertThat(result.topic()).isEqualTo(INVALID_TOPIC);
        assertThat(result.key()).isEqualTo(KEY);
        assertThat(result.value()).isEqualTo(VALUE);
    }

    @Test
    @DisplayName("Non-retryable: headers are not carried over onto the rerouted record")
    void onSendDropsHeadersOnReroutedRecord() {
        when(messageFlags.isRetryable()).thenReturn(false);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(SOURCE_TOPIC, KEY, VALUE);
        producerRecord.headers().add(new RecordHeader(ORIGINAL_TOPIC, "original-topic".getBytes(StandardCharsets.UTF_8)));

        ProducerRecord<String, Object> result = router.onSend(producerRecord);

        // The reroute uses the 3-arg ProducerRecord constructor, so the original headers
        // (including the one just added) are not propagated onto the new record.
        assertThat(result.headers().toArray()).isEmpty();
    }

    // ------------------------------------------------------------------
    // onAcknowledgement / close - no-ops
    // ------------------------------------------------------------------

    @Test
    @DisplayName("onAcknowledgement is a no-op and does not touch the message flags")
    void onAcknowledgementIsNoOp() {
        router.onAcknowledgement(null, null);

        verifyNoInteractions(messageFlags);
    }

    @Test
    @DisplayName("close is a no-op and does not touch the message flags")
    void closeIsNoOp() {
        router.close();

        verifyNoInteractions(messageFlags);
    }
}