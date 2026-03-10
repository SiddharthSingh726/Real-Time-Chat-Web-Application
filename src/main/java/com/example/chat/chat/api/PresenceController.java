package com.example.chat.chat.api;

import com.example.chat.chat.service.PresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

  private final PresenceService presenceService;

  public PresenceController(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @GetMapping("/{userId}")
  public PresenceResponse presence(@PathVariable String userId) {
    return new PresenceResponse(userId, presenceService.isOnline(userId));
  }
}
