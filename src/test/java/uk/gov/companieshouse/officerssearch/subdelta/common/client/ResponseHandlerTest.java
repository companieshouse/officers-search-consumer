package uk.gov.companieshouse.officerssearch.subdelta.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.COMPANY_APPOINTMENT_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.GET_APPOINTMENT_CALL;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.GET_OFFICER_APPOINTMENTS_CALL;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICERS_SEARCH_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.OFFICER_APPOINTMENTS_LINK;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.SEARCH_API_DELETE;
import static uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.TestUtils.SEARCH_API_PUT;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    private static final String API_ERROR_RESPONSE_MESSAGE = "%s failed, resource URI: %s, status code: %d.";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "%s failed due to invalid URI";
    private final ResponseHandler responseHandler = new ResponseHandler();

    @Test
    void handleURIValidationException() {
        // when
        Executable executable = () -> responseHandler.handle(GET_APPOINTMENT_CALL,
                new URIValidationException("Invalid URI"));

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format(URI_VALIDATION_EXCEPTION_MESSAGE, GET_APPOINTMENT_CALL), exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionRetryable() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        // when
        Executable executable = () -> responseHandler.handle(GET_APPOINTMENT_CALL, COMPANY_APPOINTMENT_LINK,
                apiErrorResponseException);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals(String.format(API_ERROR_RESPONSE_MESSAGE, GET_APPOINTMENT_CALL,
                COMPANY_APPOINTMENT_LINK, 503), exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionNonRetryableBadRequest() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(400, "Bad request", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);

        // when
        Executable executable = () -> responseHandler.handle(GET_OFFICER_APPOINTMENTS_CALL,
                OFFICER_APPOINTMENTS_LINK, apiErrorResponseException);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format(API_ERROR_RESPONSE_MESSAGE, GET_OFFICER_APPOINTMENTS_CALL,
                OFFICER_APPOINTMENTS_LINK, 400), exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionNonRetryableConflict() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(409, "Conflict", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);

        // when
        Executable executable = () -> responseHandler.handle(SEARCH_API_PUT, OFFICERS_SEARCH_LINK,
                apiErrorResponseException);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format(API_ERROR_RESPONSE_MESSAGE, SEARCH_API_PUT,
                OFFICERS_SEARCH_LINK, 409), exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionWhenNotFound() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(404, "not found", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);
        // when
        Executable executable = () -> responseHandler.handle(SEARCH_API_DELETE, OFFICERS_SEARCH_LINK,
                apiErrorResponseException);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals(String.format(API_ERROR_RESPONSE_MESSAGE, SEARCH_API_DELETE,
                OFFICERS_SEARCH_LINK, 404), exception.getMessage());
    }
}