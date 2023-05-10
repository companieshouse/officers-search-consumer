package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.stereotype.Component;

/**
 * The default service.
 */
@Component
class NullService implements Service {

    @Override
    public void processMessage(ServiceParameters parameters) {
        throw new NonRetryableException("Unable to handle message");
    }
}