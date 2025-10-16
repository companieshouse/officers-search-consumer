package uk.gov.companieshouse.officerssearch.subdelta.common.client;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private static final String GET_APPOINTMENT_CALL = "Appointments API GET Appointment";
    private static final String GET_OFFICER_APPOINTMENTS_CALL = "Appointments API GET Officer Appointments";
    private static final List<QueryParam> ITEMS_PER_PAGE_500 = List.of(new QueryParam("items_per_page", "500"));

    private final Supplier<InternalApiClient> apiClientSupplier;
    private final ResponseHandler responseHandler;

    AppointmentsApiClient(@Qualifier("apiClientSupplier") Supplier<InternalApiClient> apiClientSupplier,
            ResponseHandler responseHandler) {
        this.apiClientSupplier = apiClientSupplier;
        this.responseHandler = responseHandler;
    }

    public Optional<OfficerSummary> getAppointment(String resourceUri) {
        InternalApiClient apiClient = apiClientSupplier.get();
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
                responseHandler.handle(GET_APPOINTMENT_CALL, resourceUri, ex);
            }
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_APPOINTMENT_CALL, ex);
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

        InternalApiClient apiClient = apiClientSupplier.get();
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
                responseHandler.handle(GET_OFFICER_APPOINTMENTS_CALL, resourceUri, ex);
            }
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_OFFICER_APPOINTMENTS_CALL, ex);
        }

        return Optional.empty();
    }
}
