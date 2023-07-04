package uk.gov.companieshouse.officerssearch.subdelta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.search.PrivateSearchResourceHandler;
import uk.gov.companieshouse.api.handler.search.officers.PrivateOfficerAppointmentsSearchHandler;
import uk.gov.companieshouse.api.handler.search.officers.request.PrivateOfficerAppointmentsSearchDelete;
import uk.gov.companieshouse.api.handler.search.officers.request.PrivateOfficerAppointmentsSearchPut;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.officer.AppointmentList;

@ExtendWith(MockitoExtension.class)
class SearchApiClientTest {

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
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
    @Mock
    private PrivateOfficerAppointmentsSearchDelete privateOfficerAppointmentsSearchDelete;

    @BeforeEach
    void setup() {
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.officerSearch()).thenReturn(privateOfficerAppointmentsSearchHandler);
    }

    @Test
    @DisplayName("Should upsert officer appointments successfully with no exceptions")
    void upsertOfficerAppointments() throws Exception {
        // given
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenReturn(
                new ApiResponse<>(200, Map.of()));

        // when
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verifyNoInteractions(responseHandler);
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException (503) caught during upsert")
    void upsertOfficerAppointmentsApiErrorResponseException()
            throws Exception {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenThrow(apiErrorResponseException);

        // when
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(responseHandler).handle(String.format(
                "Error [503] in PUT appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught during upsert")
    void upsertOfficerAppointmentsIllegalArgumentException() throws Exception {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenThrow(illegalArgumentException);

        // when
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(responseHandler).handle(String.format(
                "Failed in PUT appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught during upsert")
    void upsertOfficerAppointmentsURIValidationException() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(privateOfficerAppointmentsSearchHandler.put(any(), any(AppointmentList.class)))
                .thenReturn(privateOfficerAppointmentsSearchPut);
        when(privateOfficerAppointmentsSearchPut.execute()).thenThrow(uriValidationException);

        // when
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(responseHandler).handle(String.format(
                "Failed in PUT appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), uriValidationException);
    }

    @Test
    @DisplayName("Should delete officer appointments successfully with no exceptions")
    void deleteOfficerAppointments() throws Exception {
        // given
        when(privateOfficerAppointmentsSearchHandler.delete(any()))
                .thenReturn(privateOfficerAppointmentsSearchDelete);
        when(privateOfficerAppointmentsSearchDelete.execute()).thenReturn(
                new ApiResponse<>(200, Map.of()));

        // when
        client.deleteOfficerAppointments(OFFICER_ID, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete("/officers-search/officers/" + OFFICER_ID);
        verifyNoInteractions(responseHandler);
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException (503) caught during delete")
    void deleteOfficerAppointmentsApiErrorResponseException()
            throws Exception {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        when(privateOfficerAppointmentsSearchHandler.delete(any()))
                .thenReturn(privateOfficerAppointmentsSearchDelete);
        when(privateOfficerAppointmentsSearchDelete.execute()).thenThrow(apiErrorResponseException);

        // when
        client.deleteOfficerAppointments(OFFICER_ID, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete("/officers-search/officers/" + OFFICER_ID);
        verify(responseHandler).handle(String.format(
                "Error [503] in DELETE appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught during delete")
    void deleteOfficerAppointmentsIllegalArgumentException() throws Exception {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(privateOfficerAppointmentsSearchHandler.delete(any()))
                .thenReturn(privateOfficerAppointmentsSearchDelete);
        when(privateOfficerAppointmentsSearchDelete.execute()).thenThrow(illegalArgumentException);

        // when
        client.deleteOfficerAppointments(OFFICER_ID, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete("/officers-search/officers/" + OFFICER_ID);
        verify(responseHandler).handle(String.format(
                "Failed in DELETE appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught during delete")
    void deleteOfficerAppointmentsURIValidationException() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(privateOfficerAppointmentsSearchHandler.delete(any()))
                .thenReturn(privateOfficerAppointmentsSearchDelete);
        when(privateOfficerAppointmentsSearchDelete.execute()).thenThrow(uriValidationException);

        // when
        client.deleteOfficerAppointments(OFFICER_ID, CONTEXT_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete("/officers-search/officers/" + OFFICER_ID);
        verify(responseHandler).handle(String.format(
                "Failed in DELETE appointment list to resource URI /officers-search/officers/%s with context id %s",
                OFFICER_ID, CONTEXT_ID), uriValidationException);
    }
}