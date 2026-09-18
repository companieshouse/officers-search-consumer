package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

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
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service.ResourceChangedServiceRouter;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedConsumerTest {

    @Mock
    private ResourceChangedServiceRouter router;
    @Mock
    private MessageFlags messageFlags;

    private ResourceChangedConsumer consumer;
    private Message<ResourceChangedData> message;

    @BeforeEach
    void setUp() {
        consumer = new ResourceChangedConsumer(router, messageFlags);
        message = MessageBuilder.withPayload(new ResourceChangedData()).build();
    }

    @Test
    @DisplayName("Happy path: message is routed and no retry flag is set")
    void consumeRoutesMessageToRouter() {
        consumer.consume(message);

        verify(router, times(1)).route(message);
        verifyNoMoreInteractions(messageFlags);
    }

    @Test
    @DisplayName("RetryableException: retry flag is set before the exception is rethrown")
    void consumeSetsRetryableFlagAndRethrowsOnRetryableException() {
        RetryableException retryableException = new RetryableException("transient failure");
        doThrow(retryableException).when(router).route(any());

        RetryableException thrown = assertThrows(RetryableException.class,
                () -> consumer.consume(message));

        assertThat(thrown).isSameAs(retryableException);
        verify(router, times(1)).route(message);
        verify(messageFlags, times(1)).setRetryable(true);
    }

    @Test
    @DisplayName("Non-retryable exceptions propagate without touching the retry flag")
    void consumePropagatesNonRetryableExceptionWithoutSettingFlag() {
        RuntimeException nonRetryable = new IllegalStateException("permanent failure");
        doThrow(nonRetryable).when(router).route(any());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> consumer.consume(message));

        assertThat(thrown).isSameAs(nonRetryable);
        verify(router, times(1)).route(message);
        verify(messageFlags, never()).setRetryable(true);
    }
}