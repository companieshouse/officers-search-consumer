package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.serdes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.appointment.OfficerSummary;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;

@ExtendWith(MockitoExtension.class)
class OfficerDeserialiserTest {

    public static final String OFFICER_DATA = "officer data json string";
    @InjectMocks
    private OfficerDeserialiser deserialiser;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OfficerSummary expected;


    @Test
    void shouldDeserialiseOfficerData() throws JsonProcessingException {
        // given
        when(objectMapper.readValue(anyString(), eq(OfficerSummary.class))).thenReturn(expected);

        // when
        OfficerSummary actual = deserialiser.deserialiseOfficerData(OFFICER_DATA);

        // then
        assertEquals(expected, actual);
        verify(objectMapper).readValue(OFFICER_DATA, OfficerSummary.class);
    }

    @Test
    void shouldThrowNonRetryableExceptionWhenJsonProcessingExceptionThrown() throws JsonProcessingException {
        // given
        when(objectMapper.readValue(anyString(), eq(OfficerSummary.class))).thenThrow(JsonProcessingException.class);

        // when
        Executable executable = () -> deserialiser.deserialiseOfficerData(OFFICER_DATA);

        // then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Failed to parse message payload", actual.getMessage());
        verify(objectMapper).readValue(OFFICER_DATA, OfficerSummary.class);
    }
}