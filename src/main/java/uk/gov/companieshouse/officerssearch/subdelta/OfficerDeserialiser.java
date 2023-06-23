package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
class OfficerDeserialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final ObjectMapper objectMapper;

    OfficerDeserialiser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    OfficerSummary deserialiseOfficerData(String data, String logContext) {
        try {
            return objectMapper.readValue(data, OfficerSummary.class);
        } catch (JsonProcessingException e) {
            LOGGER.errorContext(logContext, "Unable to parse payload data", e, null);
            throw new NonRetryableException("Unable to parse payload data", e);
        }
    }
}
