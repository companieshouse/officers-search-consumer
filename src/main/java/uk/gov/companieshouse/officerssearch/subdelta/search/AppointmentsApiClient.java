package uk.gov.companieshouse.officerssearch.subdelta.search;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.api.request.QueryParam;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class AppointmentsApiClient {

    private static final String GET_APPOINTMENT_FAILED_MSG = "Failed retrieving appointment for resource URI %s with context id %s";
    private static final String GET_APPOINTMENT_ERROR_MSG = "Error [%s] retrieving appointment for resource URI %s with context id %s";
    private static final String GET_APPOINTMENTS_LIST_FAILED_MSG = "Failed retrieving appointments list for resource URI %s with context id %s";
    private static final String GET_APPOINTMENTS_LIST_ERROR_MSG = "Error [%s] retrieving appointments list for resource URI %s with context id %s";
    private static final List<QueryParam> ITEMS_PER_PAGE_500 = List.of(new QueryParam("items_per_page", "500"));

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    AppointmentsApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public Optional<OfficerSummary> getAppointment(String resourceUri, String logContext) {
        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            return Optional.of(apiClient.privateCompanyAppointmentsListHandler()
                    .getCompanyAppointment(resourceUri)
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            if (HttpStatus.valueOf(ex.getStatusCode()).is4xxClientError()) {
                return Optional.empty();
            } else {
                responseHandler.handle(
                        String.format(GET_APPOINTMENT_ERROR_MSG, ex.getStatusCode(), resourceUri,
                                logContext), ex);
            }
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri, logContext), ex);
        }
        return Optional.empty();
    }

    public Optional<AppointmentList> getOfficerAppointmentsList(String resourceUri,
            String logContext) {

        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            return Optional.of(apiClient.privateOfficerAppointmentsListHandler()
                    .getAppointmentsList(resourceUri)
                    .queryParams(ITEMS_PER_PAGE_500)
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            if (HttpStatus.valueOf(ex.getStatusCode()).is4xxClientError()) {
                return Optional.empty();
            } else {
                responseHandler.handle(
                        String.format(GET_APPOINTMENTS_LIST_ERROR_MSG, ex.getStatusCode(), resourceUri,
                                logContext), ex);
            }
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENTS_LIST_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENTS_LIST_FAILED_MSG, resourceUri, logContext), ex);
        }

        return Optional.empty();
    }
}
