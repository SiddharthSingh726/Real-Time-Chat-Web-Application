package com.example.chat.chat.messaging;

import com.example.chat.chat.realtime.ChatEventBroadcaster;
import com.example.chat.chat.service.ChatMessageEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "kafka")
public class KafkaGatewayEventConsumer {

  private final ChatEventBroadcaster broadcaster;

  public KafkaGatewayEventConsumer(ChatEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @KafkaListener(
      topics = "${app.messaging.kafka.topic}",
      groupId = "${app.messaging.kafka.gateway-group-id}")
  public void onMessage(ChatMessageEvent event) {
    broadcaster.broadcast(event);
  }
}
