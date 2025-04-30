package uk.gov.companieshouse.officerssearch.subdelta.kafka;

import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Import(TestConfig.class)
public abstract class AbstractKafkaTest {

    @Container
    protected static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse(
            "confluentinc/cp-kafka:7.9.1"));
}