package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.APPOINTMENT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.COMPANY_NUMBER;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.LINKS_API;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.MESSAGE_PAYLOAD;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.company.appointment.request.PrivateOfficerGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListGet;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListHandler;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.delta.officers.AppointmentFullRecordAPI;
import uk.gov.companieshouse.api.officer.AppointmentList;

@ExtendWith(MockitoExtension.class)
class OfficerAppointmentsClientTest {

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
    private OfficerAppointmentsClient client;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private IdExtractor idExtractor;
    @Mock
    private PrivateOfficerAppointmentsListHandler appointmentsListHandler;
    @Mock
    private PrivateDeltaResourceHandler deltaResourceHandler;
    @Mock
    private PrivateOfficerAppointmentsListGet privateOfficerAppointmentsListGet;
    @Mock
    private PrivateOfficerGet privateOfficerGet;
    @Mock
    private AppointmentList appointmentList;
    @Mock
    private AppointmentFullRecordAPI appointmentFullRecordAPI;

    @BeforeEach
    void setup() {
        when(clientSupplier.get()).thenReturn(apiClient);
    }

    @Test
    @DisplayName("Should fetch company appointments list successfully with no exceptions")
    void shouldFetchAppointmentsList() throws ApiErrorResponseException, URIValidationException {
        // given
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenReturn(
                new ApiResponse<>(200, Collections.emptyMap(), appointmentList));

        // when
        Optional<AppointmentList> actual = client.getOfficerAppointmentsList(COMPANY_NUMBER,
                CONTEXT_ID);

        // then
        assertTrue(actual.isPresent());
        assertEquals(appointmentList, actual.get());
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught")
    void patchCompanyNameAndStatusApiErrorResponseException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(apiErrorResponseException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);
        // then
        verify(responseHandler).handle(String.format(
                        "Error [503] retrieving appointment full record for resource URI %s with context id %s",
                        COMPANY_NUMBER, CONTEXT_ID),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught")
    void patchCompanyNameAndStatusIllegalArgumentException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(illegalArgumentException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointment full record for resource URI %s with context id %s",
                        COMPANY_NUMBER,
                        CONTEXT_ID),
                illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught")
    void patchCompanyNameAndStatusURIValidationException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(uriValidationException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointment full record for resource URI %s with context id %s",
                        COMPANY_NUMBER,
                        CONTEXT_ID),
                uriValidationException);
    }

    @Test
    @DisplayName("Should fetch officer ID successfully with no exceptions")
    void shouldGetOfficerId() throws ApiErrorResponseException, URIValidationException {
        // given
        when(apiClient.privateDeltaCompanyAppointmentResourceHandler()).thenReturn(
                deltaResourceHandler);
        when(deltaResourceHandler.getAppointment(any())).thenReturn(
                privateOfficerGet);
        when(privateOfficerGet.execute()).thenReturn(
                new ApiResponse<>(200, Collections.emptyMap(), appointmentFullRecordAPI));
        when(appointmentFullRecordAPI.getLinks()).thenReturn(LINKS_API);
        when(idExtractor.extractCompanyNumberFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(idExtractor.extractOfficerIdFromSelfLink(any())).thenReturn(OFFICER_ID);

        // when
        Optional<String> actual = client.getOfficerId(MESSAGE_PAYLOAD);

        // then
        assertTrue(actual.isPresent());
        assertEquals(OFFICER_ID, actual.get());
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught fetching officer ID")
    void getOfficerIdApiErrorResponseException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateDeltaCompanyAppointmentResourceHandler()).thenReturn(
                deltaResourceHandler);
        when(deltaResourceHandler.getAppointment(any())).thenReturn(
                privateOfficerGet);
        when(idExtractor.extractCompanyNumberFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(privateOfficerGet.execute()).thenThrow(apiErrorResponseException);

        // when
        client.getOfficerId(MESSAGE_PAYLOAD);

        // then
        verify(responseHandler).handle(String.format(
                        "Error [503] retrieving officer ID for resource URI /company/%s/appointments/%s/full_record with context id %s",
                        COMPANY_NUMBER, APPOINTMENT_ID, CONTEXT_ID),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught fetching officer ID")
    void getOfficerIdIllegalArgumentException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(apiClient.privateDeltaCompanyAppointmentResourceHandler()).thenReturn(
                deltaResourceHandler);
        when(deltaResourceHandler.getAppointment(any())).thenReturn(
                privateOfficerGet);
        when(idExtractor.extractCompanyNumberFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(privateOfficerGet.execute()).thenThrow(illegalArgumentException);

        // when
        client.getOfficerId(MESSAGE_PAYLOAD);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving officer ID for resource URI /company/%s/appointments/%s/full_record with context id %s",
                        COMPANY_NUMBER, APPOINTMENT_ID, CONTEXT_ID),
                illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught fetching officer ID")
    void getOfficerIdURIValidationException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(apiClient.privateDeltaCompanyAppointmentResourceHandler()).thenReturn(
                deltaResourceHandler);
        when(deltaResourceHandler.getAppointment(any())).thenReturn(
                privateOfficerGet);
        when(idExtractor.extractCompanyNumberFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(privateOfficerGet.execute()).thenThrow(uriValidationException);

        // when
        client.getOfficerId(MESSAGE_PAYLOAD);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving officer ID for resource URI /company/%s/appointments/%s/full_record with context id %s",
                        COMPANY_NUMBER, APPOINTMENT_ID, CONTEXT_ID),
                uriValidationException);
    }
}