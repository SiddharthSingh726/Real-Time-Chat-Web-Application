package com.example.chat.chat.api;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.chat.chat.service.ChatMessageService;
import com.example.chat.chat.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

  private final ConversationService conversationService;
  private final ChatMessageService messageService;

  public ConversationController(ConversationService conversationService, ChatMessageService messageService) {
    this.conversationService = conversationService;
    this.messageService = messageService;
  }

  @PostMapping
  public ConversationResponse createConversation(@Valid @RequestBody CreateConversationRequest request,
                                                 Principal principal) {
    return conversationService.createConversation(requirePrincipal(principal), request);
  }

  @GetMapping
  public List<ConversationResponse> listConversations(Principal principal) {
    return conversationService.listConversations(requirePrincipal(principal));
  }

  @DeleteMapping("/{conversationId}")
  public void deleteConversation(@PathVariable UUID conversationId, Principal principal) {
    conversationService.deleteConversation(conversationId, requirePrincipal(principal));
  }

  @GetMapping("/{conversationId}/messages")
  public MessageHistoryResponse history(@PathVariable UUID conversationId,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
                                        @RequestParam(required = false) Integer limit,
                                        Principal principal) {
    conversationService.assertMember(conversationId, requirePrincipal(principal));
    return messageService.loadHistory(conversationId, before, limit);
  }

  private String requirePrincipal(Principal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Unauthenticated request");
    }
    return principal.getName();
  }
}
