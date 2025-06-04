package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Collections;
import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.officerssearch.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedDataSerialiserTest {

    @Mock
    private DatumWriter<ResourceChangedData> writer;

    @Test
    void testSerialiseResourceChangedData() {
        // given
        ResourceChangedData changedData = new ResourceChangedData("resource_kind",
                "resource_uri", "context_id", "resource_id", "data",
                new EventRecord("published_at", "event_type", Collections.emptyList()));
        try(ResourceChangedDataSerialiser serialiser = new ResourceChangedDataSerialiser()) {

            // when
            byte[] actual = serialiser.serialize("topic", changedData);

            // then
            assertThat(actual, is(notNullValue()));
        }
    }

    @Test
    void testThrowNonRetryableExceptionIfIOExceptionThrown() throws IOException {
        // given
        ResourceChangedData changedData = new ResourceChangedData("resource_kind",
                "resource_uri", "context_id", "resource_id", "data",
                new EventRecord("", "changed", Collections.emptyList()));
        ResourceChangedDataSerialiser serialiser = spy(new ResourceChangedDataSerialiser());
        doReturn(writer).when(serialiser).getDatumWriter();
        doThrow(IOException.class).when(writer).write(any(), any());

        // when
        Executable actual = () -> serialiser.serialize("topic", changedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, actual);
        assertThat(exception.getMessage(), is(equalTo("Error serialising delta")));
        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }
}
