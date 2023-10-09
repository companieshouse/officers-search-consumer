package uk.gov.companieshouse.officerssearch.subdelta.search;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.api.request.QueryParam;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class AppointmentsApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String GET_APPOINTMENT_FAILED_MSG = "Failed retrieving appointment for resource URI %s";
    private static final String GET_APPOINTMENT_ERROR_MSG = "Error [%s] retrieving appointment";
    private static final String GET_APPOINTMENTS_LIST_FAILED_MSG = "Failed retrieving appointments list for resource URI %s";
    private static final String GET_APPOINTMENTS_LIST_ERROR_MSG = "Error [%s] retrieving appointments list";
    private static final List<QueryParam> ITEMS_PER_PAGE_500 = List.of(new QueryParam("items_per_page", "500"));

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    AppointmentsApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public Optional<OfficerSummary> getAppointment(String resourceUri) {
        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            return Optional.of(apiClient.privateCompanyAppointmentsListHandler()
                    .getCompanyAppointment(resourceUri)
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            if (ex.getStatusCode() == 404) {
                return Optional.empty();
            } else {
                responseHandler.handle(
                        String.format(GET_APPOINTMENT_ERROR_MSG, ex.getStatusCode()), ex);
            }
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri), ex);
        }
        return Optional.empty();
    }

    public Optional<AppointmentList> getOfficerAppointmentsListForDelete(String resourceUri) {
        return getOfficerAppointmentsList(resourceUri, false);
    }

    public Optional<AppointmentList> getOfficerAppointmentsListForUpsert(String resourceUri) {
        return getOfficerAppointmentsList(resourceUri, true);
    }

    private Optional<AppointmentList> getOfficerAppointmentsList(String resourceUri, boolean isUpsert) {

        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            return Optional.of(apiClient.privateOfficerAppointmentsListHandler()
                    .getAppointmentsList(resourceUri)
                    .queryParams(ITEMS_PER_PAGE_500)
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            if (ex.getStatusCode() == 404) {
                if (isUpsert) {
                    LOGGER.error(ex.getMessage(), ex, DataMapHolder.getLogMap());
                }
                return Optional.empty();
            } else {
                responseHandler.handle(
                        String.format(GET_APPOINTMENTS_LIST_ERROR_MSG, ex.getStatusCode()), ex);
            }
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENTS_LIST_FAILED_MSG, resourceUri), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENTS_LIST_FAILED_MSG, resourceUri), ex);
        }

        return Optional.empty();
    }
}
