package uk.gov.companieshouse.officerssearch.subdelta;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.officer.AppointmentList;

@ExtendWith(MockitoExtension.class)
class UpsertOfficersSearchServiceTest {

    private static final String RESOURCE_URI = MESSAGE_PAYLOAD.getResourceUri();

    @Mock
    private OfficerAppointmentsClient officerAppointmentsClient;
    @Mock
    private SearchApiClient searchApiClient;
    @InjectMocks
    private UpsertOfficersSearchService upsertOffersSearchService;
    @Mock
    private AppointmentList appointmentList;

    @Test
    void shouldProcessMessage() {
        // given
        when(officerAppointmentsClient.getOfficerAppointmentsList(RESOURCE_URI,
                CONTEXT_ID)).thenReturn(Optional.of(appointmentList));

        // when
        upsertOffersSearchService.processMessage(RESOURCE_URI, OFFICER_ID, CONTEXT_ID);

        // then
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);
    }

    @Test
    void shouldNotProcessMessageWhenAppointmentListNotFound() {
        // given
        when(officerAppointmentsClient.getOfficerAppointmentsList(RESOURCE_URI,
                CONTEXT_ID)).thenReturn(Optional.empty());

        // when
        upsertOffersSearchService.processMessage(RESOURCE_URI, OFFICER_ID, CONTEXT_ID);

        // then
        verifyNoInteractions(searchApiClient);
    }
}