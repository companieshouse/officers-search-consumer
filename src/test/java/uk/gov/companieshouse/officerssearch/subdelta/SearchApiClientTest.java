package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.CONTEXT_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.OFFICER_ID;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.search.PrivateSearchResourceHandler;
import uk.gov.companieshouse.api.handler.search.officers.PrivateOfficerAppointmentsSearchHandler;
import uk.gov.companieshouse.api.handler.search.officers.request.PrivateOfficerAppointmentsSearchPut;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.officer.AppointmentList;

@ExtendWith(MockitoExtension.class)
class SearchApiClientTest {

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    private SearchApiClient client;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private PrivateSearchResourceHandler privateSearchResourceHandler;
    @Mock
    private PrivateOfficerAppointmentsSearchHandler privateOfficerAppointmentsSearchHandler;
    @Mock
    private PrivateOfficerAppointmentsSearchPut privateOfficerAppointmentsSearchPut;
    @Mock
    private AppointmentList appointmentList;
    private final ResponseHandler responseHandler = new ResponseHandler();

    @BeforeEach
    void setup() {
        client = new SearchApiClient(clientSupplier, responseHandler);
        when(clientSupplier.get()).thenReturn(apiClient);
    }

    @Test
    @DisplayName("Should upsert officer appointments successfully with no exceptions")
    void shouldUpsertOfficerAppointments() throws Exception {
        // given
        when(apiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.putSearchOfficerAppointments()).thenReturn(
                privateOfficerAppointmentsSearchHandler);
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenReturn(
                new ApiResponse<>(200, Map.of()));

        // when
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);

        // then
        verify(apiClient).privateSearchResourceHandler();
        verify(privateSearchResourceHandler).putSearchOfficerAppointments();
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(privateOfficerAppointmentsSearchPut).execute();
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException (503) caught")
    void shouldThrowRetryableExceptionWhenPutFailsWithApiErrorResponse503Exception()
            throws Exception {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(apiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.putSearchOfficerAppointments()).thenReturn(
                privateOfficerAppointmentsSearchHandler);
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenThrow(apiErrorResponseException);

        // when
        Executable executable = () -> client.upsertOfficerAppointments(OFFICER_ID, appointmentList,
                CONTEXT_ID);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals(String.format(
                "Error [503] in PUT appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), exception.getMessage());
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught")
    void patchCompanyNameAndStatusIllegalArgumentException() throws Exception {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(apiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.putSearchOfficerAppointments()).thenReturn(
                privateOfficerAppointmentsSearchHandler);
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenThrow(illegalArgumentException);

        // when
        Executable executable = () -> client.upsertOfficerAppointments(OFFICER_ID, appointmentList,
                CONTEXT_ID);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals(String.format(
                "Failed in PUT appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), exception.getMessage());
    }
}