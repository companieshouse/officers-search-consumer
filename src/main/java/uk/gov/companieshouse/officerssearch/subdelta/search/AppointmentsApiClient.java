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
    private static final String GET_APPOINTMENT_CALL = "Appointments API GET Appointment";
    private static final String GET_OFFICER_APPOINTMENTS_CALL = "Appointments API GET Officer Appointments";
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

    public Optional<AppointmentList> getOfficerAppointmentsListForGet(String resourceUri) {
        return getOfficerAppointmentsList(resourceUri, false);
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
                responseHandler.handle(GET_OFFICER_APPOINTMENTS_CALL, resourceUri, ex);
            }
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_OFFICER_APPOINTMENTS_CALL, ex);
        }

        return Optional.empty();
    }
}
