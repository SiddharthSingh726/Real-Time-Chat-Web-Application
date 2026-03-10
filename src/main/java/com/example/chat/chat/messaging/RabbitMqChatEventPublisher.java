package com.example.chat.chat.messaging;

import com.example.chat.config.MessagingProperties;
import com.example.chat.chat.service.ChatEventPublisher;
import com.example.chat.chat.service.ChatMessageEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "rabbitmq")
public class RabbitMqChatEventPublisher implements ChatEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final MessagingProperties messagingProperties;

  public RabbitMqChatEventPublisher(RabbitTemplate rabbitTemplate,
                                    MessagingProperties messagingProperties) {
    this.rabbitTemplate = rabbitTemplate;
    this.messagingProperties = messagingProperties;
  }

  @Override
  public void publish(ChatMessageEvent event) {
    rabbitTemplate.convertAndSend(messagingProperties.getRabbit().getExchange(), "", event);
  }
}
