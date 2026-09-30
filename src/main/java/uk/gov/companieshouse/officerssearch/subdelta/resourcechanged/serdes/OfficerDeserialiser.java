package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class OfficerDeserialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final JsonMapper objectMapper;

    OfficerDeserialiser(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OfficerSummary deserialiseOfficerData(String data) {
        try {
            return objectMapper.readValue(data, OfficerSummary.class);
        } catch (JacksonException e) {
            LOGGER.errorContext("Failed to parse message payload", e, DataMapHolder.getLogMap());
            throw new NonRetryableException("Failed to parse message payload", e);
        }
    }
}
