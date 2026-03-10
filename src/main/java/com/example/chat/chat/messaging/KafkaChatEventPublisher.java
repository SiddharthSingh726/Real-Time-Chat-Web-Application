package com.example.chat.chat.messaging;

import com.example.chat.config.MessagingProperties;
import com.example.chat.chat.service.ChatEventPublisher;
import com.example.chat.chat.service.ChatMessageEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "kafka")
public class KafkaChatEventPublisher implements ChatEventPublisher {

  private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
  private final MessagingProperties messagingProperties;

  public KafkaChatEventPublisher(KafkaTemplate<String, ChatMessageEvent> kafkaTemplate,
                                 MessagingProperties messagingProperties) {
    this.kafkaTemplate = kafkaTemplate;
    this.messagingProperties = messagingProperties;
  }

  @Override
  public void publish(ChatMessageEvent event) {
    kafkaTemplate.send(
        messagingProperties.getKafka().getTopic(),
        event.conversationId().toString(),
        event);
  }
}
