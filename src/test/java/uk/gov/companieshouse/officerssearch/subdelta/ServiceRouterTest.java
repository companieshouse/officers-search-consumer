package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MESSAGE_PAYLOAD;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ServiceRouterTest {

    @Mock
    private UpsertOfficersSearchService upsertOffersSearchService;
    @Mock
    private Message<ResourceChangedData> message;

    @InjectMocks
    private ServiceRouter router;

    @Test
    @DisplayName("Should call upsert officers search service when event is changed")
    void routeChangedAppointment() {
        // given
        when(message.getPayload()).thenReturn(MESSAGE_PAYLOAD);
        // when
        router.route(message);

        // then
        verify(upsertOffersSearchService).processMessage(MESSAGE_PAYLOAD);
    }

    @Test
    @DisplayName("Should throw non retryable exception when event type is not changed")
    void shouldNotProcessNonChangedEvent() {
        // given
        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder(MESSAGE_PAYLOAD)
                .clearEvent()
                .setEvent(new EventRecord("", "deleted", Collections.emptyList()))
                .build();
        when(message.getPayload()).thenReturn(resourceChangedData);

        // when
        Executable executable = () -> router.route(message);

        // then
        verifyNoInteractions(upsertOffersSearchService);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format("Unable to handle message with log context [%s]", CONTEXT_ID),
                exception.getMessage());
    }
}