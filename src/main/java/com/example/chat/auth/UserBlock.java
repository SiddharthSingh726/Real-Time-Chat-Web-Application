package com.example.chat.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_block",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_block_pair", columnNames = {
        "blocker_id", "blocked_id"
    }))
public class UserBlock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "blocker_id", nullable = false, length = 120)
  private String blockerId;

  @Column(name = "blocked_id", nullable = false, length = 120)
  private String blockedId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getBlockerId() {
    return blockerId;
  }

  public void setBlockerId(String blockerId) {
    this.blockerId = blockerId;
  }

  public String getBlockedId() {
    return blockedId;
  }

  public void setBlockedId(String blockedId) {
    this.blockedId = blockedId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
