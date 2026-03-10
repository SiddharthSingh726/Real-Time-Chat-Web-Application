package com.example.chat.chat.messaging;

import com.example.chat.chat.realtime.ChatEventBroadcaster;
import com.example.chat.chat.service.ChatMessageEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalGatewayEventConsumer {

  private final ChatEventBroadcaster broadcaster;

  public LocalGatewayEventConsumer(ChatEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @EventListener
  public void onMessage(ChatMessageEvent event) {
    broadcaster.broadcast(event);
  }
}
