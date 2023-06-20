package uk.gov.companieshouse.officerssearch.subdelta;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.officer.AppointmentList;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
class OfficerAppointmentsClient {

    private static final String GET_APPOINTMENT_FAILED_MSG = "Failed retrieving appointment full record for resource URI %s with context id %s";
    private static final String GET_APPOINTMENT_ERROR_MSG = "Error [%s] retrieving appointment full record for resource URI %s with context id %s";
    private static final String COMPANY_APPOINTMENTS_FULL_RECORD_FMT = "/company/%s/appointments/%s/full_record";
    private static final String GET_OFFICER_ID_FAILED_MSG = "Failed retrieving officer ID for resource URI %s with context id %s";
    private static final String GET_OFFICER_ID_ERROR_MSG = "Error [%s] retrieving officer ID for resource URI %s with context id %s";

    private final IdExtractor idExtractor;
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    OfficerAppointmentsClient(IdExtractor idExtractor,
            Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.idExtractor = idExtractor;
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
        } catch (Exception ex) {
            responseHandler.handle(
                    String.format(GET_APPOINTMENT_FAILED_MSG, resourceUri, logContext));
        }

        return Optional.empty();
    }

    public Optional<String> getOfficerId(ResourceChangedData payload) {
        String appointmentId = payload.getResourceId();
        String companyNumber = idExtractor.extractCompanyNumberFromUri(payload.getResourceUri());
        String resourceUri = String.format(COMPANY_APPOINTMENTS_FULL_RECORD_FMT,
                companyNumber, appointmentId);
        String logContext = payload.getContextId();
        InternalApiClient apiClient = internalApiClientFactory.get();

        try {
            String officerSelfLink = apiClient.privateDeltaCompanyAppointmentResourceHandler()
                    .getAppointment(resourceUri)
                    .execute()
                    .getData().getLinks().getOfficerLinksData().getSelfLink();

            return Optional.of(idExtractor.extractOfficerIdFromSelfLink(officerSelfLink));
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(GET_OFFICER_ID_ERROR_MSG, ex.getStatusCode(), resourceUri,
                            logContext), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(
                    String.format(GET_OFFICER_ID_FAILED_MSG, resourceUri, logContext), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(
                    String.format(GET_OFFICER_ID_FAILED_MSG, resourceUri, logContext), ex);
        } catch (Exception ex) {
            responseHandler.handle(
                    String.format(GET_OFFICER_ID_FAILED_MSG, resourceUri, logContext));
        }
        return Optional.empty();
    }

}
