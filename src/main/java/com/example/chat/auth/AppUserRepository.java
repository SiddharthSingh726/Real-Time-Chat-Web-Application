package com.example.chat.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

  boolean existsByUserId(String userId);

  Optional<AppUser> findByUserId(String userId);

  List<AppUser> findAllByOrderByDisplayNameAscUserIdAsc();
}
