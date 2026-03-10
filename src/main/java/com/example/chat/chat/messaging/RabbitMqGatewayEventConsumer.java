package com.example.chat.chat.messaging;

import com.example.chat.chat.realtime.ChatEventBroadcaster;
import com.example.chat.chat.service.ChatMessageEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "rabbitmq")
public class RabbitMqGatewayEventConsumer {

  private final ChatEventBroadcaster broadcaster;

  public RabbitMqGatewayEventConsumer(ChatEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @RabbitListener(queues = "#{gatewayQueue.name}")
  public void onMessage(ChatMessageEvent event) {
    broadcaster.broadcast(event);
  }
}
