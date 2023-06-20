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
    private static final Pattern EXTRACT_COMPANY_NUMBER_PATTERN =
            Pattern.compile("(?<=company/)([a-zA-Z0-9]{6,10})(?=/.*)");
    private static final String NULL_EMPTY_URI =
            "Could not extract company number from empty or null resource uri";
    private static final String EXTRACTION_ERROR =
            "Could not extract company number from resource URI: ";

    public String extractCompanyNumberFromUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            LOGGER.error(NULL_EMPTY_URI);
            throw new NonRetryableException(NULL_EMPTY_URI);
        }
        // matches up to 10 digits between company/ and /
        Matcher matcher = EXTRACT_COMPANY_NUMBER_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group();
        } else {
            LOGGER.error(String.format(EXTRACTION_ERROR + "%s", uri));
            throw new NonRetryableException(String.format(EXTRACTION_ERROR + "%s", uri));
        }
    }

    public String extractOfficerIdFromSelfLink(String officerSelfLink) {
        int index = officerSelfLink.lastIndexOf("/");
        if (index == -1) {
            throw new IllegalArgumentException(String.format("Invalid officer self link %s",
                    officerSelfLink));
        }

        return officerSelfLink.substring(index + 1);
    }
}
