package com.example.chat.auth;

import java.security.Principal;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blocks")
public class UserBlockController {

  private final UserBlockService userBlockService;

  public UserBlockController(UserBlockService userBlockService) {
    this.userBlockService = userBlockService;
  }

  @PostMapping("/{userId}")
  public void block(@PathVariable String userId, Principal principal) {
    userBlockService.block(requirePrincipal(principal), userId);
  }

  @DeleteMapping("/{userId}")
  public void unblock(@PathVariable String userId, Principal principal) {
    userBlockService.unblock(requirePrincipal(principal), userId);
  }

  private String requirePrincipal(Principal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Unauthenticated request");
    }
    return principal.getName();
  }
}
