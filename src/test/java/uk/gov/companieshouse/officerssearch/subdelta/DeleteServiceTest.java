package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.DELETED_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.TEST_INTERNAL_GET_PARAMS;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_APPOINTMENTS_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;

import java.util.List;
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
import uk.gov.companieshouse.api.request.QueryParam;

@ExtendWith(MockitoExtension.class)
class DeleteServiceTest {

    private static final List<QueryParam> TEST_QUERY_PARAMS = new QueryParamBuilder().build(TEST_INTERNAL_GET_PARAMS);

    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private SearchApiClient searchApiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private OfficerDeserialiser officerDeserialiser;
    @Mock
    private QueryParamBuilder queryParamBuilder;

    @InjectMocks
    private DeleteService deleteService;
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
        when(officerDeserialiser.deserialiseOfficerData(anyString(), anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsList(anyString(), anyString(), any()))
                .thenReturn(Optional.of(appointmentList));
        when(queryParamBuilder.build(any())).thenReturn(TEST_QUERY_PARAMS);

        // when
        deleteService.processMessage(DELETED_MESSAGE_PAYLOAD);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsList(OFFICER_APPOINTMENTS_LINK, CONTEXT_ID, TEST_QUERY_PARAMS);
        verify(officerDeserialiser).deserialiseOfficerData(DELETED_MESSAGE_PAYLOAD.getData(), CONTEXT_ID);
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);
    }

    @Test
    void shouldProcessMessageAndCallDeleteWhenNoAppointmentFound() {
        // given
        when(officerDeserialiser.deserialiseOfficerData(anyString(), anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsList(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(queryParamBuilder.build(any())).thenReturn(TEST_QUERY_PARAMS);

        // when
        deleteService.processMessage(DELETED_MESSAGE_PAYLOAD);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsList(OFFICER_APPOINTMENTS_LINK, CONTEXT_ID, TEST_QUERY_PARAMS);
        verify(officerDeserialiser).deserialiseOfficerData(DELETED_MESSAGE_PAYLOAD.getData(), CONTEXT_ID);
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).deleteOfficerAppointments(OFFICER_ID, CONTEXT_ID);
    }

    @Test
    void shouldThrowRetryableExceptionWhenAppointmentReturnedFromApi() {
        // given
        when(appointmentsApiClient.getAppointment(anyString(), anyString()))
                .thenReturn(Optional.of(officerSummary));

        // when
        Executable exectuable = () -> deleteService.processMessage(DELETED_MESSAGE_PAYLOAD);

        // then
        RetryableException exception = assertThrows(RetryableException.class, exectuable);
        assertEquals("Appointment has not yet been deleted", exception.getMessage());
        verify(appointmentsApiClient).getAppointment(DELETED_MESSAGE_PAYLOAD.getResourceUri(), CONTEXT_ID);
        verifyNoMoreInteractions(appointmentsApiClient);
        verifyNoInteractions(officerDeserialiser);
        verifyNoInteractions(idExtractor);
        verifyNoInteractions(searchApiClient);
    }
}