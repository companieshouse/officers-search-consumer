package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_APPOINTMENTS_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.ResourceChangedTestUtils.RESOURCE_CHANGED_MESSAGE_PAYLOAD;

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
import uk.gov.companieshouse.officerssearch.subdelta.common.client.AppointmentsApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.client.SearchApiClient;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes.OfficerDeserialiser;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedUpsertServiceTest {

    @Mock
    private AppointmentsApiClient appointmentsApiClient;
    @Mock
    private SearchApiClient searchApiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private OfficerDeserialiser officerDeserialiser;

    @InjectMocks
    private ResourceChangedUpsertService resourceChangedUpsertService;
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
        when(resourceChangedData.getData()).thenReturn(RESOURCE_CHANGED_MESSAGE_PAYLOAD.getData());
        when(officerDeserialiser.deserialiseOfficerData(anyString())).thenReturn(officerSummary);
        when(officerSummary.getLinks()).thenReturn(links);
        when(links.getOfficer()).thenReturn(officerLinks);
        when(officerLinks.getAppointments()).thenReturn(OFFICER_APPOINTMENTS_LINK);
    }

    @Test
    void shouldProcessMessage() {
        // given
        when(idExtractor.extractOfficerId(any())).thenReturn(OFFICER_ID);
        when(appointmentsApiClient.getOfficerAppointmentsListForUpsert(anyString()))
                .thenReturn(Optional.of(appointmentList));

        // when
        resourceChangedUpsertService.processMessage(resourceChangedData);

        // then
        verify(appointmentsApiClient).getOfficerAppointmentsListForUpsert(OFFICER_APPOINTMENTS_LINK);
        verify(officerDeserialiser).deserialiseOfficerData(RESOURCE_CHANGED_MESSAGE_PAYLOAD.getData());
        verify(idExtractor).extractOfficerId(OFFICER_APPOINTMENTS_LINK);
        verify(searchApiClient).upsertOfficerAppointments(OFFICER_ID, appointmentList);
    }

    @Test
    void shouldNotProcessMessageWhenAppointmentListNotFound() {
        // given
        when(appointmentsApiClient.getOfficerAppointmentsListForUpsert(anyString()))
                .thenReturn(Optional.empty());

        // when
        Executable executable = () -> resourceChangedUpsertService.processMessage(resourceChangedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals("Officer appointments unavailable", exception.getMessage());
        verify(appointmentsApiClient).getOfficerAppointmentsListForUpsert(OFFICER_APPOINTMENTS_LINK);
        verify(officerDeserialiser).deserialiseOfficerData(RESOURCE_CHANGED_MESSAGE_PAYLOAD.getData());
        verifyNoInteractions(searchApiClient);
    }
}