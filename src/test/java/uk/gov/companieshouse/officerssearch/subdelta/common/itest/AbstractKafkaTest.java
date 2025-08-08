package uk.gov.companieshouse.officerssearch.subdelta.common.itest;

import com.google.common.collect.Iterables;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@Testcontainers
@Import(TestKafkaConfig.class)
public abstract class AbstractKafkaTest {

    @Container
    private static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:latest");

    @Autowired
    public KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    public KafkaProducer<String, byte[]> testProducer;

    @Autowired
    public TestConsumerAspect testConsumerAspect;

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.subscribe(getSubscribedTopics());
        testConsumer.poll(Duration.ofMillis(1000));
    }

    public static int recordsPerTopic(ConsumerRecords<?, ?> records, String topic) {
        return Iterables.size(records.records(topic));
    }

    public abstract List<String> getSubscribedTopics();
}