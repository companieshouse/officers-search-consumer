package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.logging.DataMapHolder;

@Component
public class IdExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final Pattern OFFICER_ID_PATTERN =
            Pattern.compile("(?<=officers/)([^/]+)(?=/appointments)");
    private static final String NULL_EMPTY_URI = "Extract officer ID failed, empty or null resource URI";
    private static final String EXTRACTION_ERROR = "Extract officer ID failed, resource URI: %s";

    public String extractOfficerId(String officerAppointmentsLink) {
        if (!StringUtils.hasText(officerAppointmentsLink)) {
            LOGGER.error(NULL_EMPTY_URI, DataMapHolder.getLogMap());
            throw new NonRetryableException(NULL_EMPTY_URI);
        }

        Matcher matcher = OFFICER_ID_PATTERN.matcher(officerAppointmentsLink);
        if (matcher.find()) {
            return matcher.group();
        } else {
            LOGGER.error(String.format(EXTRACTION_ERROR, officerAppointmentsLink), DataMapHolder.getLogMap());
            throw new NonRetryableException(String.format(EXTRACTION_ERROR, officerAppointmentsLink));
        }
    }
}
