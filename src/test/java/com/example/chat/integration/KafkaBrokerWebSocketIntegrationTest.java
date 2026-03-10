package com.example.chat.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class KafkaBrokerWebSocketIntegrationTest extends AbstractBrokerWebSocketIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("chatapp")
      .withUsername("chat")
      .withPassword("chat");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Container
  static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

  @DynamicPropertySource
  static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

    registry.add("app.messaging.provider", () -> "kafka");
    registry.add("app.messaging.kafka.topic", () -> "chat.messages.it.kafka");
    registry.add("app.messaging.kafka.gateway-group-id", () -> "chat-gateway-it-" + System.nanoTime());
    registry.add("app.ai.enabled", () -> "false");
  }

  @Test
  void shouldFanoutAndAckThroughKafka() throws Exception {
    runBrokerFanoutFlow();
  }
}
