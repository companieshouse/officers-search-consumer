package uk.gov.companieshouse.officerssearch.subdelta;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
class IdExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final Pattern OFFICER_ID_PATTERN =
            Pattern.compile("(?<=officers/)([^/]+)(?=/appointments)");
    private static final String NULL_EMPTY_URI =
            "Could not extract company number from empty or null resource uri";
    private static final String EXTRACTION_ERROR =
            "Could not extract company number from resource URI: ";

    public String extractOfficerId(String officerAppointmentsLink) {
        if (!StringUtils.hasText(officerAppointmentsLink)) {
            LOGGER.error(NULL_EMPTY_URI);
            throw new NonRetryableException(NULL_EMPTY_URI);
        }

        Matcher matcher = OFFICER_ID_PATTERN.matcher(officerAppointmentsLink);
        if (matcher.find()) {
            return matcher.group();
        } else {
            LOGGER.error(String.format(EXTRACTION_ERROR + "%s", officerAppointmentsLink));
            throw new NonRetryableException(String.format(EXTRACTION_ERROR + "%s", officerAppointmentsLink));
        }
    }
}
