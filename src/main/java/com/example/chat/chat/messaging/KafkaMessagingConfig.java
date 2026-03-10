package com.example.chat.chat.messaging;

import com.example.chat.config.MessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "kafka")
public class KafkaMessagingConfig {

  @Bean
  public NewTopic chatMessagesTopic(MessagingProperties messagingProperties) {
    return TopicBuilder.name(messagingProperties.getKafka().getTopic())
        .partitions(Math.max(1, messagingProperties.getKafka().getPartitions()))
        .replicas(1)
        .build();
  }
}
