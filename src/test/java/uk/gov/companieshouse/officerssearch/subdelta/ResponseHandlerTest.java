package uk.gov.companieshouse.officerssearch.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    private final ResponseHandler responseHandler = new ResponseHandler();

    @Test
    void handleURIValidationException() {
        // when
        Executable executable = () -> responseHandler.handle("failed message",
                new URIValidationException("Invalid URI"));

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals("failed message", exception.getMessage());
    }

    @Test
    void handleIllegalArgumentException() {
        // when
        Executable executable = () -> responseHandler.handle("failed message",
                new IllegalArgumentException("Illegal Argument"));

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals("failed message", exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionRetryable() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503,
                "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        // when
        Executable executable = () -> responseHandler.handle("failed message",
                apiErrorResponseException);

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals("failed message", exception.getMessage());
    }

    @Test
    void handleApiErrorResponseExceptionNonRetryable() {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(404, "not found",
                new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(
                builder);

        // when
        Executable executable = () -> responseHandler.handle("failed message",
                apiErrorResponseException);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals("failed message", exception.getMessage());
    }

    @Test
    void shouldThrowRetryableExceptionWhenHandlingMessageWithoutAnException() {
        // given

        // when
        Executable executable = () -> responseHandler.handle("failed message");

        // then
        RetryableException exception = assertThrows(RetryableException.class, executable);
        assertEquals("failed message", exception.getMessage());
    }

}