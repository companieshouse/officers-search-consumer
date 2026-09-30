package uk.gov.companieshouse.officerssearch.subdelta.logging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

class OpenTelemetryAppenderInitializerTest {

    @Test
    void afterPropertiesSetDelegatesToInstallAppender() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OpenTelemetryAppenderInitializer initializer =
                spy(new OpenTelemetryAppenderInitializer(openTelemetry));

        // Stub out the seam so the real static
        // OpenTelemetryAppender.install(...) call, and its global
        // JVM logging side effect, is never invoked in this test.
        doNothing().when(initializer).installAppender(openTelemetry);

        initializer.afterPropertiesSet();

        verify(initializer).installAppender(openTelemetry);
    }

    @Test
    void installAppenderDelegatesToOpenTelemetryAppenderInstall() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OpenTelemetryAppenderInitializer initializer =
                new OpenTelemetryAppenderInitializer(openTelemetry);

        // Exercises the real static OpenTelemetryAppender.install(...)
        // call so the delegation itself is covered; the call only
        // registers the appender with logback and is safe to invoke
        // repeatedly in tests.
        assertThatCode(() -> initializer.installAppender(openTelemetry))
                .doesNotThrowAnyException();
    }
}
