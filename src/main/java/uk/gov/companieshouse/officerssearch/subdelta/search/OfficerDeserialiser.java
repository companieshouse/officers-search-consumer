package uk.gov.companieshouse.officerssearch.subdelta.search;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;

@Component
public class OfficerDeserialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final ObjectMapper objectMapper;

    OfficerDeserialiser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OfficerSummary deserialiseOfficerData(String data, String logContext) {
        try {
            return objectMapper.readValue(data, OfficerSummary.class);
        } catch (JsonProcessingException e) {
            LOGGER.errorContext(logContext, "Unable to parse message payload data", e, null);
            throw new NonRetryableException("Unable to parse message payload data", e);
        }
    }
}
