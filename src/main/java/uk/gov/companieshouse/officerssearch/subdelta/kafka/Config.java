package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
@EnableKafka
public class Config {

    private final MessageFlags messageFlags;
    private final String kafka2BootstrapServers;
    private final String kafka3BootstrapServers;
    private final String invalidMessageTopic;

    public Config(MessageFlags messageFlags, @Value("${spring.kafka2.bootstrap-servers}") String kafka2BootstrapServers,
            @Value("${spring.kafka3.bootstrap-servers}") String kafka3BootstrapServers,
            @Value("${invalid_message_topic}") String invalidMessageTopic) {
        this.messageFlags = messageFlags;
        this.kafka2BootstrapServers = kafka2BootstrapServers;
        this.kafka3BootstrapServers = kafka3BootstrapServers;
        this.invalidMessageTopic = invalidMessageTopic;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    Supplier<InternalApiClient> internalApiClientSupplier(
            @Value("${api.api-key}") String apiKey,
            @Value("${api.api-url}") String apiUrl) {
        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(
                    apiKey));
            internalApiClient.setBasePath(apiUrl);
            return internalApiClient;
        };
    }

    // Resource changed kafka config
    @Bean
    public ConsumerFactory<String, String> resourceChangedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka2BootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new StringDeserializer()));
    }

    @Bean
    public ProducerFactory<String, String> resourceChangedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka2BootstrapServers,
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                        InvalidMessageRouter.class.getName(),
                        "message.flags", messageFlags,
                        "invalid.message.topic", invalidMessageTopic),
                new StringSerializer(), new StringSerializer());
    }

    @Bean(name = "resourceChangedKafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplateResourceChanged() {
        return new KafkaTemplate<>(resourceChangedProducerFactory());
    }

    @Bean(name = "resourceChangedContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> resourceChangedKafkaListenerContainerFactory(
            @Value("${consumer.concurrency}") Integer concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(resourceChangedConsumerFactory());
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    // Officer Merge Kafka config
    @Bean
    public ConsumerFactory<String, String> officerMergeConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka3BootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new StringDeserializer()));
    }

    @Bean
    public ProducerFactory<String, String> officerMergeProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka2BootstrapServers,
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                        InvalidMessageRouter.class.getName(),
                        "message.flags", messageFlags,
                        "invalid.message.topic", "officer-merge-invalid"),
                new StringSerializer(), new StringSerializer());
    }

    @Bean(name = "officerMergeKafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplateOfficerMerge() {
        return new KafkaTemplate<>(officerMergeProducerFactory());
    }

    @Bean(name = "officerMergeContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> officerMergeKafkaListenerContainerFactory(
            @Value("${consumer.concurrency}") Integer concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(officerMergeConsumerFactory());
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}