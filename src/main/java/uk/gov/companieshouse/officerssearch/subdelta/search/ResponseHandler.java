package uk.gov.companieshouse.officerssearch.subdelta.search;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String API_INFO_RESPONSE_MESSAGE = "Call to API failed, status code: %d. %s";

    public void handle(String message, URIValidationException ex) {
        LOGGER.error(message, ex, DataMapHolder.getLogMap());
        throw new NonRetryableException(message, ex);
    }

    public void handle(String message, IllegalArgumentException ex) {
        String causeMessage = ex.getCause() != null
                ? String.format("; %s", ex.getCause().getMessage()) : "";
        LOGGER.info(message + causeMessage, DataMapHolder.getLogMap());
        throw new RetryableException(message, ex);
    }

    public void handle(String message, ApiErrorResponseException ex) {

        if (HttpStatus.BAD_REQUEST.value() == ex.getStatusCode() || HttpStatus.CONFLICT.value() == ex.getStatusCode()) {
            LOGGER.error(message, ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(message, ex);
        } else {
            LOGGER.info(
                    String.format(API_INFO_RESPONSE_MESSAGE, ex.getStatusCode(), Arrays.toString(ex.getStackTrace())),
                    DataMapHolder.getLogMap());
            throw new RetryableException(message, ex);
        }
    }
}
