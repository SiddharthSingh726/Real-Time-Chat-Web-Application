package com.example.chat.auth;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserDirectoryController {

  private final LocalAuthService localAuthService;
  private final UserBlockService userBlockService;

  public UserDirectoryController(LocalAuthService localAuthService,
                                 UserBlockService userBlockService) {
    this.localAuthService = localAuthService;
    this.userBlockService = userBlockService;
  }

  @GetMapping
  public List<UserProfileResponse> listUsers(Principal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Unauthenticated request");
    }
    String currentUserId = principal.getName();
    List<UserProfileResponse> users = localAuthService.listUsers().stream()
        .filter(user -> !user.userId().equals(currentUserId))
        .toList();
    Map<String, UserBlockState> states = userBlockService.getStates(
        currentUserId,
        users.stream().map(UserProfileResponse::userId).toList());
    return users.stream()
        .map(user -> {
          UserBlockState state = states.getOrDefault(user.userId(), new UserBlockState(false, false));
          return new UserProfileResponse(
              user.userId(),
              user.displayName(),
              state.blockedByMe(),
              state.hasBlockedMe(),
              state.directChatAllowed());
        })
        .toList();
  }
}
