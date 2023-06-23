package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.COMPANY_NUMBER;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;

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
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListGet;
import uk.gov.companieshouse.api.handler.officers.PrivateOfficerAppointmentsListHandler;
import uk.gov.companieshouse.api.model.ApiResponse;
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
    private PrivateOfficerAppointmentsListHandler appointmentsListHandler;
    @Mock
    private PrivateOfficerAppointmentsListGet privateOfficerAppointmentsListGet;
    @Mock
    private AppointmentList appointmentList;

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
}