package uk.gov.companieshouse.officerssearch.subdelta.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory; 

@ExtendWith(MockitoExtension.class)
class KafkaStackTraceAppenderInitializerTest {

    private static final String KAFKA_LOGGER_NAME = "org.springframework.kafka";
    private static final String APPENDER_NAME = "KAFKA_PLAIN";
    private static final String EXPECTED_PATTERN = "%d{ISO8601} %-5level %logger{60} - %msg%n%ex{full}";

    private final KafkaStackTraceAppenderInitializer initializer = new KafkaStackTraceAppenderInitializer();

    @Mock
    private LoggerContext loggerContext;
    @Mock
    private Logger kafkaLogger;
    @Captor
    private ArgumentCaptor<Appender<ILoggingEvent>> appenderCaptor;

    @Test
    void shouldAttachConsoleAppenderToSpringKafkaLogger() {
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(LoggerFactory.class)) {
            loggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);
            when(loggerContext.getLogger(KAFKA_LOGGER_NAME)).thenReturn(kafkaLogger);

            initializer.afterPropertiesSet();

            verify(loggerContext).getLogger(KAFKA_LOGGER_NAME);
            verify(kafkaLogger).addAppender(appenderCaptor.capture());
            verifyNoMoreInteractions(kafkaLogger);

            Appender<ILoggingEvent> appender = appenderCaptor.getValue();
            assertInstanceOf(ConsoleAppender.class, appender);
            assertEquals(APPENDER_NAME, appender.getName());
            assertTrue(appender.isStarted());
            assertSame(loggerContext, appender.getContext());
        }
    }

    @Test
    void shouldConfigureStartedPatternLayoutEncoderWithFullStackTracePattern() {
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(LoggerFactory.class)) {
            loggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);
            when(loggerContext.getLogger(KAFKA_LOGGER_NAME)).thenReturn(kafkaLogger);

            initializer.afterPropertiesSet();

            verify(kafkaLogger).addAppender(appenderCaptor.capture());

            ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) appenderCaptor.getValue();
            PatternLayoutEncoder encoder = assertInstanceOf(PatternLayoutEncoder.class, appender.getEncoder());
            assertEquals(EXPECTED_PATTERN, encoder.getPattern());
            assertTrue(encoder.isStarted());
            assertSame(loggerContext, encoder.getContext());
        }
    }

    @Test
    void shouldAttachAFreshAppenderOnEachInvocation() {
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(LoggerFactory.class)) {
            loggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(loggerContext);
            when(loggerContext.getLogger(KAFKA_LOGGER_NAME)).thenReturn(kafkaLogger);

            initializer.afterPropertiesSet();
            initializer.afterPropertiesSet();

            verify(kafkaLogger, org.mockito.Mockito.times(2)).addAppender(appenderCaptor.capture());
            assertEquals(2, appenderCaptor.getAllValues().size());
            org.junit.jupiter.api.Assertions.assertNotSame(
                    appenderCaptor.getAllValues().get(0), appenderCaptor.getAllValues().get(1));
        }
    }
}
