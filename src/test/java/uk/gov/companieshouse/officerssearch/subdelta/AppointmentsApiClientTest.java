package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.COMPANY_APPOINTMENT_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.COMPANY_NUMBER;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.appointment.PrivateCompanyAppointment;
import uk.gov.companieshouse.api.handler.appointment.PrivateCompanyAppointmentsListHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListGet;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListHandler;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.api.request.QueryParam;

@ExtendWith(MockitoExtension.class)
class AppointmentsApiClientTest {

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
    private AppointmentsApiClient client;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private PrivateOfficerAppointmentsListHandler appointmentsListHandler;
    @Mock
    private PrivateOfficerAppointmentsListGet privateOfficerAppointmentsListGet;
    @Mock
    private PrivateCompanyAppointmentsListHandler privateCompanyAppointmentsListHandler;
    @Mock
    private PrivateCompanyAppointment privateCompanyAppointment;
    @Mock
    private AppointmentList appointmentList;
    @Mock
    private OfficerSummary officerSummary;

    @Captor
    private ArgumentCaptor<List<QueryParam>> queryParamCaptor;

    @BeforeEach
    void setup() {
        when(clientSupplier.get()).thenReturn(apiClient);
    }

    @Test
    @DisplayName("Should fetch company appointment successfully with no exceptions")
    void shouldFetchAppointment() throws ApiErrorResponseException, URIValidationException {
        // given
        when(apiClient.privateCompanyAppointmentsListHandler()).thenReturn(privateCompanyAppointmentsListHandler);
        when(privateCompanyAppointmentsListHandler.getCompanyAppointment(any())).thenReturn(privateCompanyAppointment);
        when(privateCompanyAppointment.execute()).thenReturn(
                new ApiResponse<>(200, Collections.emptyMap(), officerSummary));

        // when
        Optional<OfficerSummary> actual = client.getAppointment(COMPANY_APPOINTMENT_LINK, CONTEXT_ID);

        // then
        assertTrue(actual.isPresent());
        assertEquals(officerSummary, actual.get());
        verify(privateCompanyAppointmentsListHandler).getCompanyAppointment(COMPANY_APPOINTMENT_LINK);
    }

    @Test
    @DisplayName("Should return empty optional ApiErrorResponseException caught and response code 404 not found")
    void shouldReturnEmptyOptionalGetAppointment404NotFound()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(404,
                "not found", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateCompanyAppointmentsListHandler()).thenReturn(privateCompanyAppointmentsListHandler);
        when(privateCompanyAppointmentsListHandler.getCompanyAppointment(any())).thenReturn(privateCompanyAppointment);
        when(privateCompanyAppointment.execute()).thenThrow(apiErrorResponseException);

        // when
        Optional<OfficerSummary> actual = client.getAppointment(COMPANY_APPOINTMENT_LINK, CONTEXT_ID);

        // then
        assertTrue(actual.isEmpty());
        verify(privateCompanyAppointmentsListHandler).getCompanyAppointment(COMPANY_APPOINTMENT_LINK);
        verifyNoInteractions(responseHandler);
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught")
    void getAppointmentApiErrorResponseException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateCompanyAppointmentsListHandler()).thenReturn(privateCompanyAppointmentsListHandler);
        when(privateCompanyAppointmentsListHandler.getCompanyAppointment(any())).thenReturn(privateCompanyAppointment);
        when(privateCompanyAppointment.execute()).thenThrow(apiErrorResponseException);

        // when
        client.getAppointment(COMPANY_APPOINTMENT_LINK, CONTEXT_ID);
        // then
        verify(privateCompanyAppointmentsListHandler).getCompanyAppointment(COMPANY_APPOINTMENT_LINK);
        verify(responseHandler).handle(String.format(
                        "Error [503] retrieving appointment for resource URI %s with context id %s",
                        COMPANY_APPOINTMENT_LINK, CONTEXT_ID),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught")
    void getAppointmentIllegalArgumentException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(apiClient.privateCompanyAppointmentsListHandler()).thenReturn(privateCompanyAppointmentsListHandler);
        when(privateCompanyAppointmentsListHandler.getCompanyAppointment(any())).thenReturn(privateCompanyAppointment);
        when(privateCompanyAppointment.execute()).thenThrow(illegalArgumentException);

        // when
        client.getAppointment(COMPANY_APPOINTMENT_LINK, CONTEXT_ID);

        // then
        verify(privateCompanyAppointmentsListHandler).getCompanyAppointment(COMPANY_APPOINTMENT_LINK);
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointment for resource URI %s with context id %s",
                        COMPANY_APPOINTMENT_LINK,
                        CONTEXT_ID),
                illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught")
    void getAppointmentURIValidationException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(apiClient.privateCompanyAppointmentsListHandler()).thenReturn(privateCompanyAppointmentsListHandler);
        when(privateCompanyAppointmentsListHandler.getCompanyAppointment(any())).thenReturn(privateCompanyAppointment);
        when(privateCompanyAppointment.execute()).thenThrow(uriValidationException);

        // when
        client.getAppointment(COMPANY_APPOINTMENT_LINK, CONTEXT_ID);

        // then
        verify(privateCompanyAppointmentsListHandler).getCompanyAppointment(COMPANY_APPOINTMENT_LINK);
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointment for resource URI %s with context id %s",
                        COMPANY_APPOINTMENT_LINK,
                        CONTEXT_ID),
                uriValidationException);
    }

    @Test
    @DisplayName("Should fetch company appointments list successfully with no exceptions")
    void shouldFetchAppointmentsList() throws ApiErrorResponseException, URIValidationException {
        // given
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.queryParams(any())).thenReturn(privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenReturn(
                new ApiResponse<>(200, Collections.emptyMap(), appointmentList));

        // when
        Optional<AppointmentList> actual = client.getOfficerAppointmentsList(COMPANY_NUMBER,
                CONTEXT_ID);

        // then
        assertTrue(actual.isPresent());
        assertEquals(appointmentList, actual.get());
        verify(privateOfficerAppointmentsListGet).queryParams(queryParamCaptor.capture());
        QueryParam queryParamArgument = queryParamCaptor.getValue().get(0);
        assertEquals("items_per_page", queryParamArgument.getKey());
        assertEquals("500", queryParamArgument.getValue());
    }

    @Test
    @DisplayName("Should return empty optional ApiErrorResponseException caught and response code 404 not found")
    void fetchAppointmentListApiErrorResponseException404NotFound()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(404,
                "not found", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.queryParams(any())).thenReturn(privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(apiErrorResponseException);

        // when
        Optional<AppointmentList> actual = client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);

        // then
        assertTrue(actual.isEmpty());
        verifyNoInteractions(responseHandler);
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught")
    void fetchAppointmentListApiErrorResponseException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.queryParams(any())).thenReturn(privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(apiErrorResponseException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);
        // then
        verify(responseHandler).handle(String.format(
                        "Error [503] retrieving appointments list for resource URI %s with context id %s",
                        COMPANY_NUMBER, CONTEXT_ID),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught")
    void fetchAppointmentListIllegalArgumentException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.queryParams(any())).thenReturn(privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(illegalArgumentException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointments list for resource URI %s with context id %s",
                        COMPANY_NUMBER,
                        CONTEXT_ID),
                illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught")
    void fetchAppointmentListURIValidationException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(apiClient.privateOfficerAppointmentsListHandler()).thenReturn(appointmentsListHandler);
        when(appointmentsListHandler.getAppointmentsList(any())).thenReturn(
                privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.queryParams(any())).thenReturn(privateOfficerAppointmentsListGet);
        when(privateOfficerAppointmentsListGet.execute()).thenThrow(uriValidationException);

        // when
        client.getOfficerAppointmentsList(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(responseHandler).handle(String.format(
                        "Failed retrieving appointments list for resource URI %s with context id %s",
                        COMPANY_NUMBER,
                        CONTEXT_ID),
                uriValidationException);
    }
}