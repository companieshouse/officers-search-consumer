package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.DELETED_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.MESSAGE_PAYLOAD;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedResourceChangedServiceRouterTest {

    @Mock
    private ResourceChangedUpsertService resourceChangedUpsertService;
    @Mock
    private ResourceChangedDeleteService resourceChangedDeleteService;
    @Mock
    private Message<ResourceChangedData> message;

    @InjectMocks
    private ResourceChangedServiceRouter router;

    @Test
    @DisplayName("Should call upsert service when event type is changed")
    void routeChangedAppointment() {
        // given
        when(message.getPayload()).thenReturn(MESSAGE_PAYLOAD);
        // when
        router.route(message);

        // then
        verify(resourceChangedUpsertService).processMessage(MESSAGE_PAYLOAD);
    }

    @Test
    @DisplayName("Should call delete service when event type is deleted")
    void routeDeletedAppointment() {
        // given
        when(message.getPayload()).thenReturn(DELETED_MESSAGE_PAYLOAD);
        // when
        router.route(message);

        // then
        verify(resourceChangedDeleteService).processMessage(DELETED_MESSAGE_PAYLOAD);
    }

    @Test
    @DisplayName("Should throw non retryable exception when event type is not changed or deleted")
    void shouldNotProcessNonChangedEvent() {
        // given
        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder(MESSAGE_PAYLOAD)
                .clearEvent()
                .setEvent(new EventRecord("", "bad event type", Collections.emptyList()))
                .build();
        when(message.getPayload()).thenReturn(resourceChangedData);

        // when
        Executable executable = () -> router.route(message);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format("Unable to handle message with log context [%s]", CONTEXT_ID),
                exception.getMessage());
        verifyNoInteractions(resourceChangedUpsertService);
        verifyNoInteractions(resourceChangedDeleteService);
    }
}