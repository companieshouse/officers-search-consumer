package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public void handle(String message, URIValidationException ex) {
        LOGGER.error(message);
        throw new NonRetryableException(message, ex);
    }

    public void handle(String message, IllegalArgumentException ex) {
        String causeMessage = ex.getCause() != null
                ? String.format("; %s", ex.getCause().getMessage()) : "";
        LOGGER.info(message + causeMessage);
        throw new RetryableException(message, ex);
    }

    public void handle(String message, ApiErrorResponseException ex) {
        if (HttpStatus.valueOf(ex.getStatusCode()).is5xxServerError()) {
            LOGGER.info(message);
            throw new RetryableException(message, ex);
        } else {
            LOGGER.error(message);
            throw new NonRetryableException(message, ex);
        }
    }
}
