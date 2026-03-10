package com.example.chat.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

  @Mock
  private UserBlockRepository userBlockRepository;

  @Mock
  private AppUserRepository appUserRepository;

  private UserBlockService service;

  @BeforeEach
  void setUp() {
    service = new UserBlockService(userBlockRepository, appUserRepository);
  }

  @Test
  void rejectsDirectConversationWhenEitherUserBlockedTheOther() {
    when(userBlockRepository.existsByBlockerIdAndBlockedId("user-1", "user-2")).thenReturn(false);
    when(userBlockRepository.existsByBlockerIdAndBlockedId("user-2", "user-1")).thenReturn(true);

    assertThrows(AccessDeniedException.class, () -> service.assertDirectConversationAllowed("user-1", "user-2"));
  }

  @Test
  void rejectsBlockingYourself() {
    assertThrows(IllegalArgumentException.class, () -> service.block("user-1", "user-1"));

    verify(appUserRepository, never()).existsByUserId("user-1");
  }
}
