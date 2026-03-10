package com.example.chat.auth;

public record UserBlockState(
    boolean blockedByMe,
    boolean hasBlockedMe
) {

  public boolean directChatAllowed() {
    return !blockedByMe && !hasBlockedMe;
  }

  public boolean anyBlock() {
    return blockedByMe || hasBlockedMe;
  }
}
