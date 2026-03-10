package com.example.chat.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final LocalAuthService localAuthService;

  public AuthController(LocalAuthService localAuthService) {
    this.localAuthService = localAuthService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return localAuthService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return localAuthService.login(request);
  }

  @GetMapping("/me")
  public UserProfileResponse me(@RequestHeader(value = LocalAuthService.AUTH_TOKEN_HEADER, required = false) String authToken,
                                @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    String token = authToken;
    if ((token == null || token.isBlank()) && authorization != null && authorization.startsWith("Bearer ")) {
      token = authorization.substring("Bearer ".length()).trim();
    }
    if (token == null || token.isBlank()) {
      throw new AccessDeniedException("Missing auth token");
    }
    return localAuthService.me(token);
  }
}
