package uk.gov.companieshouse.officerssearch.subdelta;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;

@Component
class SearchApiClient {

    private static final String PUT_APPOINTMENT_LIST_FAILED_MSG = "Failed in PUT appointment list to resource URI %s with context id %s";
    private static final String PUT_APPOINTMENT_LIST_ERROR_MSG = "Error [%s] in PUT appointment list to resource URI %s with context id %s";
    private static final String DELETE_APPOINTMENT_LIST_FAILED_MSG = "Failed in DELETE appointment list to resource URI %s with context id %s";
    private static final String DELETE_APPOINTMENT_LIST_ERROR_MSG = "Error [%s] in DELETE appointment list to resource URI %s with context id %s";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    SearchApiClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void upsertOfficerAppointments(String officerId, AppointmentList appointmentList, String logContext) {
        String resourceUri = String.format("/officers-search/officers/%s", officerId);
        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            apiClient.privateSearchResourceHandler()
                    .officerSearch()
                    .put(resourceUri, appointmentList)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(PUT_APPOINTMENT_LIST_ERROR_MSG, ex.getStatusCode(), resourceUri, logContext), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(PUT_APPOINTMENT_LIST_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(PUT_APPOINTMENT_LIST_FAILED_MSG, resourceUri, logContext), ex);
        }
    }

    public void deleteOfficerAppointments(String officerId, String logContext) {
        String resourceUri = String.format("/officers-search/officers/%s", officerId);
        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            apiClient.privateSearchResourceHandler()
                    .officerSearch()
                    .delete(resourceUri)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(DELETE_APPOINTMENT_LIST_ERROR_MSG, ex.getStatusCode(), resourceUri, logContext), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(DELETE_APPOINTMENT_LIST_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(DELETE_APPOINTMENT_LIST_FAILED_MSG, resourceUri, logContext), ex);
        }
    }
}
