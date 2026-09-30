package uk.gov.companieshouse.officerssearch.subdelta.officermerge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.officermerge.service.OfficerMergeService;

@ExtendWith(MockitoExtension.class)
class OfficerMergeConsumerTest {

    // Constructor accepts OfficerMergeService specifically (not the wider MergeService
    // field type it's assigned to), so the mock is typed to match the constructor.
    @Mock
    private OfficerMergeService router;
    @Mock
    private MessageFlags messageFlags;

    private OfficerMergeConsumer consumer;
    private Message<OfficerMerge> message;

    @BeforeEach
    void setUp() {
        consumer = new OfficerMergeConsumer(router, messageFlags);
        message = MessageBuilder.withPayload(new OfficerMerge()).build();
    }

    @Test
    @DisplayName("Happy path: message is processed and no retry flag is set")
    void consumeProcessesMessageWithRouter() {
        consumer.consume(message);

        verify(router, times(1)).processMessage(message);
        verifyNoMoreInteractions(messageFlags);
    }

    @Test
    @DisplayName("RetryableException: retry flag is set before the exception is rethrown")
    void consumeSetsRetryableFlagAndRethrowsOnRetryableException() {
        RetryableException retryableException = new RetryableException("transient failure");
        doThrow(retryableException).when(router).processMessage(any());

        RetryableException thrown = assertThrows(RetryableException.class,
                () -> consumer.consume(message));

        assertThat(thrown).isSameAs(retryableException);
        verify(router, times(1)).processMessage(message);
        verify(messageFlags, times(1)).setRetryable(true);
    }

    @Test
    @DisplayName("Non-retryable exceptions propagate without touching the retry flag")
    void consumePropagatesNonRetryableExceptionWithoutSettingFlag() {
        RuntimeException nonRetryable = new IllegalStateException("permanent failure");
        doThrow(nonRetryable).when(router).processMessage(any());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> consumer.consume(message));

        assertThat(thrown).isSameAs(nonRetryable);
        verify(router, times(1)).processMessage(message);
        verify(messageFlags, never()).setRetryable(true);
    }
}