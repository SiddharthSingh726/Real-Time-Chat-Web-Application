package com.example.chat.chat.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "chat_message",
    uniqueConstraints = @UniqueConstraint(name = "uk_chat_message_client_id", columnNames = {
        "conversation_id", "sender_id", "client_message_id"
    }))
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @Column(name = "sender_id", nullable = false, length = 120)
  private String senderId;

  @Column(name = "client_message_id", length = 120)
  private String clientMessageId;

  @Column(name = "body", nullable = false, length = 4000)
  private String body;

  @Column(name = "ai_generated", nullable = false)
  private boolean aiGenerated;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Conversation getConversation() {
    return conversation;
  }

  public void setConversation(Conversation conversation) {
    this.conversation = conversation;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getClientMessageId() {
    return clientMessageId;
  }

  public void setClientMessageId(String clientMessageId) {
    this.clientMessageId = clientMessageId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public boolean isAiGenerated() {
    return aiGenerated;
  }

  public void setAiGenerated(boolean aiGenerated) {
    this.aiGenerated = aiGenerated;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
