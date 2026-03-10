package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RealtimeChatApplication {

  public static void main(String[] args) {
    SpringApplication.run(RealtimeChatApplication.class, args);
  }
}
