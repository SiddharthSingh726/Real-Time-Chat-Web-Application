package com.example.chat.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.example.chat.chat.api.ChatMessageResponse;
import com.example.chat.chat.api.MessageHistoryResponse;
import com.example.chat.chat.domain.ChatMessage;
import com.example.chat.chat.domain.Conversation;
import com.example.chat.chat.repo.ChatMessageRepository;
import com.example.chat.chat.repo.ConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatMessageService {

  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 200;

  private final ChatMessageRepository messageRepository;
  private final ConversationRepository conversationRepository;
  private final ChatEventPublisher chatEventPublisher;
  private final ApplicationEventPublisher internalEventPublisher;

  public ChatMessageService(ChatMessageRepository messageRepository,
                            ConversationRepository conversationRepository,
                            ChatEventPublisher chatEventPublisher,
                            ApplicationEventPublisher internalEventPublisher) {
    this.messageRepository = messageRepository;
    this.conversationRepository = conversationRepository;
    this.chatEventPublisher = chatEventPublisher;
    this.internalEventPublisher = internalEventPublisher;
  }

  @Transactional
  public ChatMessageResponse persistUserMessage(UUID conversationId,
                                                String senderId,
                                                String clientMessageId,
                                                String text) {
    if (StringUtils.hasText(clientMessageId)) {
      ChatMessage existing = messageRepository
          .findByConversation_IdAndSenderIdAndClientMessageId(conversationId, senderId, clientMessageId)
          .orElse(null);
      if (existing != null) {
        return toResponse(existing);
      }
    }

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

    ChatMessage message = new ChatMessage();
    message.setConversation(conversation);
    message.setSenderId(senderId);
    message.setClientMessageId(clientMessageId);
    message.setBody(text);
    message.setAiGenerated(false);
    conversation.setUpdatedAt(Instant.now());

    ChatMessage saved = messageRepository.save(message);
    publish(saved);
    return toResponse(saved);
  }

  @Transactional
  public ChatMessageResponse persistAiMessage(UUID conversationId, String text) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

    ChatMessage message = new ChatMessage();
    message.setConversation(conversation);
    message.setSenderId("ai-assistant");
    message.setClientMessageId("ai-" + UUID.randomUUID());
    message.setBody(text);
    message.setAiGenerated(true);
    conversation.setUpdatedAt(Instant.now());

    ChatMessage saved = messageRepository.save(message);
    publish(saved);
    return toResponse(saved);
  }

  public MessageHistoryResponse loadHistory(UUID conversationId, Instant before, Integer limit) {
    int safeLimit = sanitizeLimit(limit);
    List<ChatMessage> rows = before == null
        ? messageRepository.findByConversation_IdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, safeLimit))
        : messageRepository.findByConversation_IdAndCreatedAtLessThanOrderByCreatedAtDesc(
            conversationId,
            before,
            PageRequest.of(0, safeLimit));

    List<ChatMessageResponse> responses = new ArrayList<>(rows.size());
    for (ChatMessage row : rows) {
      responses.add(toResponse(row));
    }
    Collections.reverse(responses);

    Instant nextCursor = rows.isEmpty() ? null : rows.get(rows.size() - 1).getCreatedAt();
    return new MessageHistoryResponse(responses, nextCursor);
  }

  private int sanitizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.min(MAX_LIMIT, Math.max(1, limit));
  }

  private void publish(ChatMessage message) {
    ChatMessageEvent event = new ChatMessageEvent(
        message.getId(),
        message.getConversation().getId(),
        message.getSenderId(),
        message.getClientMessageId(),
        message.getBody(),
        message.getCreatedAt(),
        message.isAiGenerated());
    internalEventPublisher.publishEvent(event);
    chatEventPublisher.publish(event);
  }

  private ChatMessageResponse toResponse(ChatMessage message) {
    return new ChatMessageResponse(
        message.getId(),
        message.getConversation().getId(),
        message.getSenderId(),
        message.getClientMessageId(),
        message.getBody(),
        message.getCreatedAt(),
        message.isAiGenerated());
  }
}
