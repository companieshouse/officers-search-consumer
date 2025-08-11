package uk.gov.companieshouse.officerssearch.subdelta.common.client;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class SearchApiClient {

    private static final String SEARCH_API_PUT = "Officer Search API PUT";
    private static final String SEARCH_API_DELETE = "Officer Search API DELETE";
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    SearchApiClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void upsertOfficerAppointments(String officerId, AppointmentList appointmentList) {
        String resourceUri = String.format("/officers-search/officers/%s", officerId);
        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            apiClient.privateSearchResourceHandler()
                    .officerSearch()
                    .put(resourceUri, appointmentList)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(SEARCH_API_PUT, resourceUri, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(SEARCH_API_PUT, ex);
        }
    }

    public void deleteOfficerAppointments(String officerId) {
        String resourceUri = String.format("/officers-search/officers/%s", officerId);
        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            apiClient.privateSearchResourceHandler()
                    .officerSearch()
                    .delete(resourceUri)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(SEARCH_API_DELETE, resourceUri, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(SEARCH_API_DELETE, ex);
        }
    }
}
