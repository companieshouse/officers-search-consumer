package uk.gov.companieshouse.officerssearch.subdelta.logging;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Aspect
class StructuredLoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing delta";
    private static final String LOG_MESSAGE_PROCESSED = "Processed delta";
    private static final String EXCEPTION_MESSAGE = "%s exception thrown: %s";

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) 
        throws Throwable {

        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            DataMapHolder.initialise(extractContextId(message.getPayload())
                    .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .topic((String) message.getHeaders().get("kafka_receivedTopic"))
                    .partition((Integer) message.getHeaders().get("kafka_receivedPartitionId"))
                    .offset((Long)message.getHeaders().get("kafka_offset"));
            LOGGER.debug(LOG_MESSAGE_RECEIVED, DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.debug(LOG_MESSAGE_PROCESSED, DataMapHolder.getLogMap());

            return result;
        } catch (Exception ex) {
            LOGGER.debug(String.format(EXCEPTION_MESSAGE,
                ex.getClass().getSimpleName(), ex.getMessage()), 
                DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }


    private Optional<String> extractContextId(Object payload) {
        if (payload instanceof ResourceChangedData) {
            ResourceChangedData data = (ResourceChangedData) payload;
            Map<String, Object> logmap = DataMapHolder.getLogMap();
            logmap.put("appointment_id", data.getResourceId());
            logmap.put("request_id", data.getContextId());
            LOGGER.info("Processing appointment", logmap);
            return Optional.of(data.getContextId());
        }
        return Optional.empty();
    }
}