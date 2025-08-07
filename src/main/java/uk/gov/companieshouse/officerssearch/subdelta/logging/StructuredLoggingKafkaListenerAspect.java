package uk.gov.companieshouse.officerssearch.subdelta.logging;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.NonRetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
@Aspect
class StructuredLoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing delta";
    private static final String LOG_MESSAGE_PROCESSED = "Processed delta";
    private static final String EXCEPTION_MESSAGE = "%s exception thrown: %s";
    private final int maxAttempts;

    StructuredLoggingKafkaListenerAspect(@Value("${consumer.max_attempts}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) 
        throws Throwable {

        int retryCount = 0;
        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            retryCount = Optional.ofNullable((Integer) joinPoint.getArgs()[1]).orElse(1) - 1;
            DataMapHolder.initialise(Optional.of(extractContextId(message.getPayload()).getContextId())
                    .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .retryCount(retryCount)
                    .topic((String) joinPoint.getArgs()[2])
                    .partition((Integer) joinPoint.getArgs()[3])
                    .offset((Long) joinPoint.getArgs()[4]);
            LOGGER.info(LOG_MESSAGE_RECEIVED, DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.info(LOG_MESSAGE_PROCESSED, DataMapHolder.getLogMap());

            return result;
        }  catch (RetryableException ex) {
            // maxAttempts includes first attempt which is not a retry
            if (retryCount >= maxAttempts - 1) {
                LOGGER.error("Max retry attempts reached", ex, DataMapHolder.getLogMap());
            } else {
                LOGGER.info(String.format(EXCEPTION_MESSAGE,
                                ex.getClass().getSimpleName(), Arrays.toString(ex.getStackTrace())),
                        DataMapHolder.getLogMap());
            }
            throw ex;
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }


    private ResourceChangedData extractContextId(Object payload) {
        if (payload instanceof ResourceChangedData) {
            ResourceChangedData data = (ResourceChangedData) payload;
            Map<String, Object> logmap = DataMapHolder.getLogMap();
            logmap.put("appointment_id", data.getResourceId());
            logmap.put("request_id", data.getContextId());
            return data;
        }
        throw new NonRetryableException(String.format("Invalid payload type. Payload: %s", payload.toString()));    }
}