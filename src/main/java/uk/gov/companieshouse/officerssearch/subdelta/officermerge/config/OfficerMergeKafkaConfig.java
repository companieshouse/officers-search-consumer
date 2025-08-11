package uk.gov.companieshouse.officerssearch.subdelta.officermerge.config;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import uk.gov.companieshouse.officermerge.OfficerMerge;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.InvalidMessageRouter;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.RetryableException;
import uk.gov.companieshouse.officerssearch.subdelta.common.serdes.KafkaPayloadDeserialiser;
import uk.gov.companieshouse.officerssearch.subdelta.common.serdes.KafkaPayloadSerialiser;

@Configuration
@EnableKafka
public class OfficerMergeKafkaConfig {

    @Bean
    public ConsumerFactory<String, OfficerMerge> officerMergeConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaPayloadDeserialiser.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new KafkaPayloadDeserialiser<>(OfficerMerge.class)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OfficerMerge> officerMergeKafkaListenerContainerFactory(
            @Value("${officer-merge.consumer.concurrency}") Integer concurrency,
            ConsumerFactory<String, OfficerMerge> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OfficerMerge> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> officerMergeProducerFactory(MessageFlags messageFlags,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${officer-merge.consumer.topic}") String topic,
            @Value("${consumer.group-id}") String groupId) {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.CLIENT_ID_CONFIG, "%s-%s-producer".formatted(topic, groupId),
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DelegatingByTypeSerializer.class,
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, InvalidMessageRouter.class.getName(),
                        "message-flags", messageFlags,
                        "invalid-message-topic", "%s-%s-invalid".formatted(topic, groupId)),
                new StringSerializer(),
                new DelegatingByTypeSerializer(
                        Map.of(
                                byte[].class, new ByteArraySerializer(),
                                OfficerMerge.class, new KafkaPayloadSerialiser<>(OfficerMerge.class))));
    }

    @Bean
    public KafkaTemplate<String, Object> officerMergeKafkaTemplate(
            @Qualifier("officerMergeProducerFactory") ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public RetryTopicConfiguration retryTopicConfiguration(
            @Qualifier("officerMergeKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${officer-merge.consumer.topic}") String topic,
            @Value("${consumer.group-id}") String groupId,
            @Value("${consumer.max-attempts}") int attempts,
            @Value("${consumer.backoff-delay}") int delay) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .doNotAutoCreateRetryTopics() // this is necessary to prevent failing connection during loading of spring app context
                .includeTopic(topic)
                .maxAttempts(attempts)
                .fixedBackOff(delay)
                .useSingleTopicForSameIntervals()
                .retryTopicSuffix("-%s-retry".formatted(groupId))
                .dltSuffix("-%s-error".formatted(groupId))
                .dltProcessingFailureStrategy(DltStrategy.FAIL_ON_ERROR)
                .retryOn(RetryableException.class)
                .create(kafkaTemplate);
    }
}