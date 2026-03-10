package com.example.chat.chat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.chat.auth.AppUserRepository;
import com.example.chat.auth.UserBlockService;
import com.example.chat.auth.UserBlockState;
import com.example.chat.chat.api.ConversationResponse;
import com.example.chat.chat.api.CreateConversationRequest;
import com.example.chat.chat.domain.Conversation;
import com.example.chat.chat.domain.ConversationType;
import com.example.chat.chat.domain.ConversationMember;
import com.example.chat.chat.domain.MemberRole;
import com.example.chat.chat.repo.ConversationMemberRepository;
import com.example.chat.chat.repo.ConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationMemberRepository memberRepository;
  private final MembershipService membershipService;
  private final AppUserRepository appUserRepository;
  private final UserBlockService userBlockService;

  public ConversationService(ConversationRepository conversationRepository,
                             ConversationMemberRepository memberRepository,
                             MembershipService membershipService,
                             AppUserRepository appUserRepository,
                             UserBlockService userBlockService) {
    this.conversationRepository = conversationRepository;
    this.memberRepository = memberRepository;
    this.membershipService = membershipService;
    this.appUserRepository = appUserRepository;
    this.userBlockService = userBlockService;
  }

  @Transactional
  public ConversationResponse createConversation(String creatorId, CreateConversationRequest request) {
    Set<String> dedupedMembers = new LinkedHashSet<>(request.memberIds());
    dedupedMembers.add(creatorId);

    if (!appUserRepository.existsByUserId(creatorId)) {
      throw new IllegalArgumentException("Create an account before starting conversations");
    }

    for (String memberId : dedupedMembers) {
      if (!appUserRepository.existsByUserId(memberId)) {
        throw new IllegalArgumentException("Unknown member ID: " + memberId);
      }
    }

    if (request.type() == ConversationType.DIRECT && dedupedMembers.size() != 2) {
      throw new IllegalArgumentException("Direct conversations require exactly two unique members");
    }

    if (request.type() == ConversationType.DIRECT) {
      String otherUserId = dedupedMembers.stream()
          .filter(memberId -> !memberId.equals(creatorId))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Direct conversations require exactly one other member"));
      userBlockService.assertDirectConversationAllowed(creatorId, otherUserId);
      return conversationRepository.findExistingDirectConversation(dedupedMembers)
          .map(existing -> toResponse(existing, new ArrayList<>(dedupedMembers), creatorId))
          .orElseGet(() -> createNewConversation(creatorId, request, dedupedMembers));
    }

    return createNewConversation(creatorId, request, dedupedMembers);
  }

  private ConversationResponse createNewConversation(String creatorId,
                                                     CreateConversationRequest request,
                                                     Set<String> dedupedMembers) {
    Conversation conversation = new Conversation();
    conversation.setTitle(request.title());
    conversation.setType(request.type());
    Conversation savedConversation = conversationRepository.save(conversation);

    List<ConversationMember> members = new ArrayList<>();
    for (String memberId : dedupedMembers) {
      ConversationMember member = new ConversationMember();
      member.setConversation(savedConversation);
      member.setUserId(memberId);
      member.setRole(memberId.equals(creatorId) ? MemberRole.ADMIN : MemberRole.MEMBER);
      members.add(member);
    }
    memberRepository.saveAll(members);

    for (String memberId : dedupedMembers) {
      membershipService.invalidate(savedConversation.getId(), memberId);
    }

    return toResponse(savedConversation, new ArrayList<>(dedupedMembers), creatorId);
  }

  @Transactional
  public List<ConversationResponse> listConversations(String userId) {
    List<Conversation> conversations = conversationRepository.findAllForUserOrdered(userId);
    if (conversations.isEmpty()) {
      return List.of();
    }

    List<UUID> conversationIds = conversations.stream()
        .map(Conversation::getId)
        .toList();

    Map<UUID, List<String>> membersByConversation = new HashMap<>();
    Map<UUID, MemberRole> currentUserRoles = new HashMap<>();
    for (ConversationMember member : memberRepository.findByConversation_IdIn(conversationIds)) {
      membersByConversation
          .computeIfAbsent(member.getConversation().getId(), ignored -> new ArrayList<>())
          .add(member.getUserId());
      if (member.getUserId().equals(userId)) {
        currentUserRoles.put(member.getConversation().getId(), member.getRole());
      }
    }

    Set<String> directPartners = conversations.stream()
        .filter(conversation -> conversation.getType() == ConversationType.DIRECT)
        .map(conversation -> membersByConversation.getOrDefault(conversation.getId(), List.of()).stream()
            .filter(memberId -> !memberId.equals(userId))
            .findFirst()
            .orElse(null))
        .filter(memberId -> memberId != null && !memberId.isBlank())
        .collect(Collectors.toSet());
    Map<String, UserBlockState> blockStates = userBlockService.getStates(userId, directPartners);

    return conversations.stream()
        .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed())
        .map(conversation -> {
          List<String> memberIds = membersByConversation.getOrDefault(conversation.getId(), List.of()).stream()
              .sorted()
              .collect(Collectors.toList());
          boolean blocked = false;
          if (conversation.getType() == ConversationType.DIRECT) {
            String otherUserId = memberIds.stream()
                .filter(memberId -> !memberId.equals(userId))
                .findFirst()
                .orElse(null);
            blocked = otherUserId != null
                && blockStates.getOrDefault(otherUserId, new UserBlockState(false, false)).anyBlock();
          }
          return new ConversationResponse(
              conversation.getId(),
              conversation.getTitle(),
              conversation.getType(),
              conversation.getCreatedAt(),
              conversation.getUpdatedAt(),
              memberIds,
              currentUserRoles.getOrDefault(conversation.getId(), MemberRole.MEMBER) == MemberRole.ADMIN,
              blocked);
        })
        .toList();
  }

  @Transactional
  public void deleteConversation(UUID conversationId, String userId) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
    ConversationMember membership = memberRepository.findByConversation_IdAndUserId(conversationId, userId)
        .orElseThrow(() -> new AccessDeniedException("User is not a member of this conversation"));

    if (conversation.getType() == ConversationType.DIRECT) {
      memberRepository.delete(membership);
      membershipService.invalidate(conversationId, userId);
      if (memberRepository.countByConversation_Id(conversationId) == 0) {
        conversationRepository.delete(conversation);
      }
      return;
    }

    if (membership.getRole() == MemberRole.ADMIN) {
      for (ConversationMember member : memberRepository.findByConversation_Id(conversationId)) {
        membershipService.invalidate(conversationId, member.getUserId());
      }
      conversationRepository.delete(conversation);
      return;
    }

    memberRepository.delete(membership);
    membershipService.invalidate(conversationId, userId);
    if (memberRepository.countByConversation_Id(conversationId) == 0) {
      conversationRepository.delete(conversation);
    }
  }

  private ConversationResponse toResponse(Conversation conversation, List<String> memberIds, String currentUserId) {
    boolean blocked = false;
    if (conversation.getType() == ConversationType.DIRECT) {
      String otherUserId = memberIds.stream()
          .filter(memberId -> !memberId.equals(currentUserId))
          .findFirst()
          .orElse(null);
      blocked = otherUserId != null && userBlockService.getState(currentUserId, otherUserId).anyBlock();
    }
    return new ConversationResponse(
        conversation.getId(),
        conversation.getTitle(),
        conversation.getType(),
        conversation.getCreatedAt(),
        conversation.getUpdatedAt(),
        memberIds,
        true,
        blocked);
  }

  public void assertMember(UUID conversationId, String userId) {
    membershipService.assertMember(conversationId, userId);
  }

  public void assertMessagingAllowed(UUID conversationId, String userId) {
    membershipService.assertMember(conversationId, userId);
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
    if (conversation.getType() != ConversationType.DIRECT) {
      return;
    }
    List<ConversationMember> members = memberRepository.findByConversation_Id(conversationId);
    String otherUserId = members.stream()
        .map(ConversationMember::getUserId)
        .filter(memberId -> !memberId.equals(userId))
        .findFirst()
        .orElse(null);
    if (otherUserId != null) {
      userBlockService.assertDirectConversationAllowed(userId, otherUserId);
    }
  }
}
