package uk.gov.companieshouse.officerssearch.subdelta.common.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICERS_SEARCH_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.OFFICER_ID;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.SEARCH_API_DELETE;
import static uk.gov.companieshouse.officerssearch.subdelta.common.TestUtils.SEARCH_API_PUT;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.search.PrivateSearchResourceHandler;
import uk.gov.companieshouse.api.handler.search.officers.PrivateOfficerAppointmentsSearchHandler;
import uk.gov.companieshouse.api.handler.search.officers.request.PrivateOfficerAppointmentsSearchDelete;
import uk.gov.companieshouse.api.handler.search.officers.request.PrivateOfficerAppointmentsSearchPut;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.officer.AppointmentList;

@ExtendWith(MockitoExtension.class)
class SearchApiClientTest {

    private SearchApiClient client;
    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private InternalApiClient apiClient;
    @Mock
    private HttpClient httpClient;
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
        client = new SearchApiClient(clientSupplier, responseHandler, "/officers-search/officers/%s");
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.getHttpClient()).thenReturn(httpClient);
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
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList);

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
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(responseHandler).handle(SEARCH_API_PUT, OFFICERS_SEARCH_LINK, apiErrorResponseException);
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
        client.upsertOfficerAppointments(OFFICER_ID, appointmentList);

        // then
        verify(privateOfficerAppointmentsSearchHandler).put(
                eq("/officers-search/officers/" + OFFICER_ID), any(AppointmentList.class));
        verify(responseHandler).handle(SEARCH_API_PUT, uriValidationException);
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
        client.deleteOfficerAppointments(OFFICER_ID);

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
        client.deleteOfficerAppointments(OFFICER_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete(OFFICERS_SEARCH_LINK);
        verify(responseHandler).handle(SEARCH_API_DELETE, OFFICERS_SEARCH_LINK, apiErrorResponseException);
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
        client.deleteOfficerAppointments(OFFICER_ID);

        // then
        verify(privateOfficerAppointmentsSearchHandler).delete(OFFICERS_SEARCH_LINK);
        verify(responseHandler).handle(SEARCH_API_DELETE, uriValidationException);
    }
}