package uk.gov.companieshouse.officerssearch.subdelta.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;

class IdExtractorTest {

    private final IdExtractor extractor = new IdExtractor();

    @Test
    @DisplayName("The extractor should get the correct officer id")
    void shouldExtractCompanyNumber() {
        // given
        // when
        String actual = extractor.extractOfficerId(
                "/officers/-0YatipCW4ZL295N9UVFo1TGyW8/appointments");

        // then
        assertEquals("-0YatipCW4ZL295N9UVFo1TGyW8", actual);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("extractorFixtures")
    void shouldNoExtractCompanyNumberWhenPatternDoesNotMatch(String displayName, String uri,
            String expected) {
        // given

        // when
        Executable executable = () -> extractor.extractOfficerId(uri);

        // then
        Exception exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(expected, exception.getMessage());
    }

    private static Stream<Arguments> extractorFixtures() {
        return Stream.of(
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract an officer ID",
                        "company-appointments",
                        "Extract officer ID failed, resource URI: company-appointments"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract an empty officer ID",
                        "/officers//appointments",
                        "Extract officer ID failed, resource URI: /officers//appointments"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract an empty officer ID",
                        "/officers/123456/abcdef/appointments",
                        "Extract officer ID failed, resource URI: /officers/123456/abcdef/appointments"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract a officer ID from an empty uri",
                        "",
                        "Extract officer ID failed, empty or null resource URI"),
                arguments(
                        "The extractor should throw a non retryable exception when it cannot extract a officer ID from a null uri",
                        null,
                        "Extract officer ID failed, empty or null resource URI"));
    }
}
