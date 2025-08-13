package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_APPOINTMENTS_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD;

import java.util.Optional;
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
import uk.gov.companieshouse.officerssearch.subdelta.common.client.AppointmentsApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.SearchApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes.OfficerDeserialiser;

@ExtendWith(MockitoExtension.class)
class ResourceChangedDeleteServiceTest {

    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private SearchApiClient searchApiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private OfficerDeserialiser officerDeserialiser;

    @InjectMocks
    private ResourceChangedDeleteService resourceChangedDeleteService;
    @Mock
    private AppointmentList appointmentList;
    @Mock
    private OfficerSummary officerSummary;
    @Mock
    private ItemLinkTypes links;
    @Mock
    private OfficerLinkTypes officerLinks;

    @Test
    void shouldProcessMessage() {
        // given
        when(officerDeserialiser.deserialiseOfficerData(anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsListForDelete(anyString()))
                .thenReturn(Optional.of(appointmentList));

        // when
        resourceChangedDeleteService.processMessage(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsListForDelete(OFFICER_APPOINTMENTS_LINK);
        verify(officerDeserialiser).deserialiseOfficerData(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD.getData());
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList);
    }

    @Test
    void shouldProcessMessageAndCallDeleteWhenNoAppointmentFound() {
        // given
        when(officerDeserialiser.deserialiseOfficerData(anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsListForDelete(anyString()))
                .thenReturn(Optional.empty());

        // when
        resourceChangedDeleteService.processMessage(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsListForDelete(OFFICER_APPOINTMENTS_LINK);
        verify(officerDeserialiser).deserialiseOfficerData(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD.getData());
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).deleteOfficerAppointments(OFFICER_ID);
    }

    @Test
    void shouldThrowRetryableExceptionWhenAppointmentReturnedFromApi() {
        // given
        when(appointmentsApiClient.getAppointment(anyString()))
                .thenReturn(Optional.of(officerSummary));

        // when
        Executable executable = () -> resourceChangedDeleteService.processMessage(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals("Appointment has not yet been deleted", exception.getMessage());
        verify(appointmentsApiClient).getAppointment(RESOURCE_CHANGED_DELETED_MESSAGE_PAYLOAD.getResourceUri());
        verifyNoMoreInteractions(appointmentsApiClient);
        verifyNoInteractions(officerDeserialiser);
        verifyNoInteractions(idExtractor);
        verifyNoInteractions(searchApiClient);
    }
}