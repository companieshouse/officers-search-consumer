package uk.gov.companieshouse.officerssearch.subdelta.common.exception;

/**
 * Exception to handle when an invalid payload is sent to the kafka topic.
 */
public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
