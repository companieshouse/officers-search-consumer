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
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_APPOINTMENTS_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;

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
class UpsertServiceTest {

    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private SearchApiClient searchApiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private OfficerDeserialiser officerDeserialiser;

    @InjectMocks
    private UpsertService upsertService;
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
    void setup() {
        when(resourceChangedData.getContextId()).thenReturn(CONTEXT_ID);
        when(resourceChangedData.getData()).thenReturn(MESSAGE_PAYLOAD.getData());
        when(officerDeserialiser.deserialiseOfficerData(anyString(), anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
    }

    @Test
    void shouldProcessMessage() {
        // given
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsList(anyString(), anyString()))
                .thenReturn(Optional.of(appointmentList));

        // when
        upsertService.processMessage(resourceChangedData);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsList(OFFICER_APPOINTMENTS_LINK, CONTEXT_ID);
        verify(officerDeserialiser).deserialiseOfficerData(MESSAGE_PAYLOAD.getData(), CONTEXT_ID);
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);
    }

    @Test
    void shouldNotProcessMessageWhenAppointmentListNotFound() {
        // given
        when(appointmentsApiClient.getOfficerAppointmentsList(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // when
        Executable executable = () -> upsertService.processMessage(resourceChangedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals("Officer appointments unavailable", exception.getMessage());
        verify(appointmentsApiClient).getOfficerAppointmentsList(OFFICER_APPOINTMENTS_LINK, CONTEXT_ID);
        verify(officerDeserialiser).deserialiseOfficerData(MESSAGE_PAYLOAD.getData(), CONTEXT_ID);
        verifyNoInteractions(idExtractor);
        verifyNoInteractions(searchApiClient);
    }
}