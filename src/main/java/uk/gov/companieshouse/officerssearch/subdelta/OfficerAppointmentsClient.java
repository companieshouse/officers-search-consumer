package uk.gov.companieshouse.officerssearch.subdelta;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;

@Component
class OfficerAppointmentsClient {

    private static final String GET_APPOINTMENT_FAILED_MSG = "Failed retrieving appointment full record for resource URI %s with context id %s";
    private static final String GET_APPOINTMENT_ERROR_MSG = "Error [%s] retrieving appointment full record for resource URI %s with context id %s";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    OfficerAppointmentsClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public Optional<AppointmentList> getOfficerAppointmentsList(String resourceUri,
            String logContext) {

        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            return Optional.of(apiClient.privateOfficerAppointmentsListHandler()
                    .getAppointmentsList(resourceUri)
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_ERROR_MSG, ex.getStatusCode(), resourceUri,
                            logContext), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri, logContext), ex);
        }

        return Optional.empty();
    }
}
