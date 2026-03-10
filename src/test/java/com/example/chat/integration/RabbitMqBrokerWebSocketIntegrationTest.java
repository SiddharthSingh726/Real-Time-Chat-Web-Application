package com.example.chat.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqBrokerWebSocketIntegrationTest extends AbstractBrokerWebSocketIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("chatapp")
      .withUsername("chat")
      .withPassword("chat");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Container
  static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");

  @DynamicPropertySource
  static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);

    registry.add("app.messaging.provider", () -> "rabbitmq");
    registry.add("app.messaging.rabbit.exchange", () -> "chat.messages.exchange.it");
    registry.add("app.messaging.rabbit.gateway-queue-prefix", () -> "chat.gateway.it");
    registry.add("app.ai.enabled", () -> "false");
  }

  @Test
  void shouldFanoutAndAckThroughRabbitMq() throws Exception {
    runBrokerFanoutFlow();
  }
}
