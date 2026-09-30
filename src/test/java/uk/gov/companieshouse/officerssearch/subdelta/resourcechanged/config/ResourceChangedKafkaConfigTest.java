package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.InvalidMessageRouter;
import uk.gov.companieshouse.officerssearch.subdelta.common.exception.MessageFlags;
import uk.gov.companieshouse.officerssearch.subdelta.common.serdes.KafkaPayloadDeserialiser;
import uk.gov.companieshouse.officerssearch.subdelta.common.serdes.KafkaPayloadSerialiser;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedKafkaConfigTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "stream-company-officers";
    private static final String GROUP_ID = "officers-search-consumer";
    private static final int CONCURRENCY = 4;
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_DELAY = 200;

    private ResourceChangedKafkaConfig config;

    @Mock
    private MessageFlags messageFlags;
    @Mock
    private ConsumerFactory<String, ResourceChangedData> consumerFactory;
    @Mock
    private ProducerFactory<String, Object> producerFactory;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        config = new ResourceChangedKafkaConfig();
    }

    // ------------------------------------------------------------------
    // resourceChangedConsumerFactory
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Consumer factory is a DefaultKafkaConsumerFactory carrying the expected config")
    void resourceChangedConsumerFactoryReturnsConfiguredFactory() {
        ConsumerFactory<String, ResourceChangedData> factory =
                config.resourceChangedConsumerFactory(BOOTSTRAP_SERVERS);

        assertNotNull(factory);
        assertThat(factory).isInstanceOf(DefaultKafkaConsumerFactory.class);

        Map<String, Object> props = factory.getConfigurationProperties();
        assertEquals(BOOTSTRAP_SERVERS, props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(ErrorHandlingDeserializer.class,
                props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(ErrorHandlingDeserializer.class,
                props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class,
                props.get(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS));
        assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals("false", props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
    }

    @Test
    @DisplayName("Consumer factory delegates to a payload-aware ErrorHandlingDeserializer")
    void resourceChangedConsumerFactoryUsesErrorHandlingDeserialisers() {
        ConsumerFactory<String, ResourceChangedData> factory =
                config.resourceChangedConsumerFactory(BOOTSTRAP_SERVERS);

        assertThat(factory.getKeyDeserializer()).isInstanceOf(StringDeserializer.class);
        assertThat(factory.getValueDeserializer()).isInstanceOf(ErrorHandlingDeserializer.class);
        assertEquals(KafkaPayloadDeserialiser.class,
                factory.getConfigurationProperties()
                        .get(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS));

        Object delegate = ReflectionTestUtils.getField(factory.getValueDeserializer(), "delegate");
        assertThat(delegate).isInstanceOf(KafkaPayloadDeserialiser.class);
    }

    // ------------------------------------------------------------------
    // resourceChangedKafkaListenerContainerFactory
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Listener container factory wires the consumer factory, concurrency and RECORD ack mode")
    void resourceChangedKafkaListenerContainerFactoryReturnsConfiguredFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ResourceChangedData> factory =
                config.resourceChangedKafkaListenerContainerFactory(CONCURRENCY, consumerFactory);

        assertNotNull(factory);
        assertSame(consumerFactory, factory.getConsumerFactory());
        assertEquals(AckMode.RECORD, factory.getContainerProperties().getAckMode());
        assertEquals(CONCURRENCY, ReflectionTestUtils.getField(factory, "concurrency"));
    }

    // ------------------------------------------------------------------
    // resourceChangedProducerFactory
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Producer factory is a DefaultKafkaProducerFactory carrying the expected config")
    void resourceChangedProducerFactoryReturnsConfiguredFactory() {
        ProducerFactory<String, Object> factory = config.resourceChangedProducerFactory(
                messageFlags, BOOTSTRAP_SERVERS, TOPIC, GROUP_ID);

        assertNotNull(factory);
        assertThat(factory).isInstanceOf(DefaultKafkaProducerFactory.class);

        Map<String, Object> props = factory.getConfigurationProperties();
        assertEquals("%s-%s-producer".formatted(TOPIC, GROUP_ID),
                props.get(ProducerConfig.CLIENT_ID_CONFIG));
        assertEquals(BOOTSTRAP_SERVERS, props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("all", props.get(ProducerConfig.ACKS_CONFIG));
        assertEquals(StringSerializer.class, props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(DelegatingByTypeSerializer.class,
                props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        assertEquals(InvalidMessageRouter.class.getName(),
                props.get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG));
        assertSame(messageFlags, props.get("message-flags"));
        assertEquals("%s-%s-invalid".formatted(TOPIC, GROUP_ID), props.get("invalid-message-topic"));
    }

    @Test
    @DisplayName("Producer factory delegates byte[] and payload types to the right serialisers")
    void resourceChangedProducerFactoryUsesDelegatingByTypeSerializer() {
        ProducerFactory<String, Object> factory = config.resourceChangedProducerFactory(
                messageFlags, BOOTSTRAP_SERVERS, TOPIC, GROUP_ID);

        assertThat(factory.getKeySerializer()).isInstanceOf(StringSerializer.class);
        assertThat(factory.getValueSerializer()).isInstanceOf(DelegatingByTypeSerializer.class);

        @SuppressWarnings("unchecked")
        Map<Class<?>, Object> delegates = (Map<Class<?>, Object>) ReflectionTestUtils.getField(
                factory.getValueSerializer(), "delegates");

        assertNotNull(delegates);
        assertThat(delegates.get(byte[].class)).isInstanceOf(ByteArraySerializer.class);
        assertThat(delegates.get(ResourceChangedData.class))
                .isInstanceOf(KafkaPayloadSerialiser.class);
    }

    // ------------------------------------------------------------------
    // resourceChangedKafkaTemplate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Kafka template is built from the supplied producer factory")
    void resourceChangedKafkaTemplateReturnsTemplateBackedByProducerFactory() {
        KafkaTemplate<String, Object> template = config.resourceChangedKafkaTemplate(producerFactory);

        assertNotNull(template);
        assertSame(producerFactory, template.getProducerFactory());
    }

    // ------------------------------------------------------------------
    // resourceChangedRetryTopicConfiguration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Retry topic configuration targets the consumer topic")
    void resourceChangedRetryTopicConfigurationIncludesConsumerTopic() {
        RetryTopicConfiguration retryTopicConfiguration =
                config.resourceChangedRetryTopicConfiguration(
                        kafkaTemplate, TOPIC, GROUP_ID, MAX_ATTEMPTS, BACKOFF_DELAY);

        assertNotNull(retryTopicConfiguration);
        assertTrue(retryTopicConfiguration.hasConfigurationForTopics(new String[]{TOPIC}));
        assertThat(retryTopicConfiguration.hasConfigurationForTopics(new String[]{"some-other-topic"}))
                .isFalse();
    }

    @Test
    @DisplayName("Retry topic configuration collapses same-interval retries into one topic plus a DLT")
    void resourceChangedRetryTopicConfigurationUsesSingleRetryTopicAndDlt() {
        RetryTopicConfiguration retryTopicConfiguration =
                config.resourceChangedRetryTopicConfiguration(
                        kafkaTemplate, TOPIC, GROUP_ID, MAX_ATTEMPTS, BACKOFF_DELAY);

        // main topic + one aggregated retry topic + DLT
        assertEquals(3, retryTopicConfiguration.getDestinationTopicProperties().size());
        assertThat(retryTopicConfiguration.getDestinationTopicProperties())
                .extracting(properties -> ReflectionTestUtils.getField(properties, "suffix"))
                .containsExactly("",
                        "-%s-retry".formatted(GROUP_ID),
                        "-%s-error".formatted(GROUP_ID));
    }

    @Test
    @DisplayName("Retry topic configuration does not auto-create retry topics at context load")
    void resourceChangedRetryTopicConfigurationDoesNotAutoCreateTopics() {
        RetryTopicConfiguration retryTopicConfiguration =
                config.resourceChangedRetryTopicConfiguration(
                        kafkaTemplate, TOPIC, GROUP_ID, MAX_ATTEMPTS, BACKOFF_DELAY);

        // TopicCreation's accessors are protected, so read the flag reflectively
        Object topicCreation = ReflectionTestUtils.invokeMethod(
                retryTopicConfiguration, "forKafkaTopicAutoCreation");
        assertNotNull(topicCreation);
        assertEquals(false, ReflectionTestUtils.getField(topicCreation, "shouldCreateTopics"));
    }
}