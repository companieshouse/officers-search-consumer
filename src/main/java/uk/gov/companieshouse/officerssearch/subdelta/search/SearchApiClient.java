package uk.gov.companieshouse.officerssearch.subdelta.search;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class SearchApiClient {

    private static final String PUT_APPOINTMENT_LIST_FAILED_MSG = "Failed in PUT appointment list to resource URI %s";
    private static final String PUT_APPOINTMENT_LIST_ERROR_MSG = "Error [%s] in PUT appointment list to resource URI %s";
    private static final String DELETE_APPOINTMENT_LIST_FAILED_MSG = "Failed in DELETE appointment list to resource URI %s";
    private static final String DELETE_APPOINTMENT_LIST_ERROR_MSG = "Error [%s] in DELETE appointment list to resource URI %s";

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
            responseHandler.handle(
                    String.format(PUT_APPOINTMENT_LIST_ERROR_MSG, ex.getStatusCode(), resourceUri), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(PUT_APPOINTMENT_LIST_FAILED_MSG, resourceUri), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(PUT_APPOINTMENT_LIST_FAILED_MSG, resourceUri), ex);
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
            responseHandler.handle(
                    String.format(DELETE_APPOINTMENT_LIST_ERROR_MSG, ex.getStatusCode(), resourceUri), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(DELETE_APPOINTMENT_LIST_FAILED_MSG, resourceUri), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(DELETE_APPOINTMENT_LIST_FAILED_MSG, resourceUri), ex);
        }
    }
}
