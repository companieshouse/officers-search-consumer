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
    private static final String API_INFO_RESPONSE_MESSAGE = "%s failed, resource URI: %s, status code: %d. %s";
    private static final String API_ERROR_RESPONSE_MESSAGE = "%s failed, resource URI: %s, status code: %d.";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "%s failed due to invalid URI";

    public void handle(String apiCall, URIValidationException ex) {
        LOGGER.error(String.format(URI_VALIDATION_EXCEPTION_MESSAGE, apiCall), ex, DataMapHolder.getLogMap());
        throw new NonRetryableException(String.format(URI_VALIDATION_EXCEPTION_MESSAGE, apiCall), ex);
    }

    public void handle(String apiCall, String resourceUri, ApiErrorResponseException ex) {
        final int statusCode = ex.getStatusCode();
        if (HttpStatus.BAD_REQUEST.value() == statusCode || HttpStatus.CONFLICT.value() == statusCode) {
            LOGGER.error(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode),
                    ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode), ex);
        } else {
            LOGGER.info(
                    String.format(API_INFO_RESPONSE_MESSAGE, apiCall, resourceUri, ex.getStatusCode(),
                            Arrays.toString(ex.getStackTrace())),
                    DataMapHolder.getLogMap());
            throw new RetryableException(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode), ex);
        }
    }
}
