package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.messagePayloadBytes;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.appointment.ItemLinkTypes;
import uk.gov.companieshouse.api.appointment.OfficerLinkTypes;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class UpsertOfficersSearchServiceTest {

    private static final String RESOURCE_URI = MESSAGE_PAYLOAD.getResourceUri();

    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private SearchApiClient searchApiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private OfficerDeserialiser officerDeserialiser;

    @InjectMocks
    private UpsertOfficersSearchService upsertOffersSearchService;
    @Mock
    private AppointmentList appointmentList;
    @Mock
    private ResourceChangedData resourceChangedData;
    @Mock
    private OfficerSummary officerSummary;
    @Mock
    private ItemLinkTypes links;
    @Mock
    private OfficerLinkTypes officerLinks;

    @BeforeEach
    void setup() throws JsonProcessingException {
        when(resourceChangedData.getContextId()).thenReturn(CONTEXT_ID);
        when(resourceChangedData.getData()).thenReturn(
                new String(messagePayloadBytes(MESSAGE_PAYLOAD)));
        when(idExtractor.extractOfficerIdFromSelfLink(any())).thenReturn(OFFICER_ID);
        when(officerDeserialiser.deserialiseOfficerData(anyString(), anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getSelf()).thenReturn(OFFICER_ID);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getSelf()).thenReturn(RESOURCE_URI);
    }

    @Test
    void shouldProcessMessage() {
        // given
        when(appointmentsApiClient.getOfficerAppointmentsList(RESOURCE_URI,
                CONTEXT_ID)).thenReturn(Optional.of(appointmentList));

        // when
        upsertOffersSearchService.processMessage(resourceChangedData);

        // then
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);
    }

    @Test
    void shouldNotProcessMessageWhenAppointmentListNotFound() {
        // given
        when(appointmentsApiClient.getOfficerAppointmentsList(RESOURCE_URI,
                CONTEXT_ID)).thenReturn(Optional.empty());

        // when
        Executable exectuable = () -> upsertOffersSearchService.processMessage(resourceChangedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, exectuable);
        assertEquals("Officer appointments unavailable", exception.getMessage());
        verifyNoInteractions(searchApiClient);
    }
}