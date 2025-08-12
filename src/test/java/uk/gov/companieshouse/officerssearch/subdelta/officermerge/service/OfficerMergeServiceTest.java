package uk.gov.companieshouse.officerssearch.subdelta.officermerge.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_APPOINTMENTS_LINK_MERGE;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_MERGE_MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.PREVIOUS_OFFICER_ID;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.AppointmentsApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.SearchApiClient;

@ExtendWith(MockitoExtension.class)
class OfficerMergeServiceTest {

    @Mock
    private Message<OfficerMerge> officerMergeMessage;
    @Mock
    private SearchApiClient searchClient;
    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private AppointmentList appointmentList;

    @InjectMocks
    private OfficerMergeService officerMergeService;

    @Test
    void shouldUpsertOfficerAppointmentsToPrimarySearchApiIfAnyAppointmentsFoundForPreviousOfficerId() {
        // given
        when(officerMergeMessage.getPayload()).thenReturn(OFFICER_MERGE_MESSAGE_PAYLOAD);
        when(appointmentsApiClient.getOfficerAppointmentsListForMerge(anyString())).thenReturn(Optional.of(appointmentList));

        // when
        officerMergeService.processMessage(officerMergeMessage);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsListForMerge(OFFICER_APPOINTMENTS_LINK_MERGE);
        verify(searchClient).upsertOfficerAppointments(PREVIOUS_OFFICER_ID, appointmentList);
    }

    @Test
    void shouldDeleteOfficerAppointmentsIfNoAppointmentsFoundForPreviousOfficerId() {
        // given
        when(officerMergeMessage.getPayload()).thenReturn(OFFICER_MERGE_MESSAGE_PAYLOAD);
        when(appointmentsApiClient.getOfficerAppointmentsListForMerge(anyString())).thenReturn(Optional.empty());

        // when
        officerMergeService.processMessage(officerMergeMessage);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsListForMerge(OFFICER_APPOINTMENTS_LINK_MERGE);
        verify(searchClient).deleteOfficerAppointments(PREVIOUS_OFFICER_ID);
    }
}
