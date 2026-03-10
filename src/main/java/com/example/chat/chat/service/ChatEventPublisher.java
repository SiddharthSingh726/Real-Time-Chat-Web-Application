package com.example.chat.chat.service;

public interface ChatEventPublisher {

  void publish(ChatMessageEvent event);
}
