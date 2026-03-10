package com.example.chat.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties({
    SecurityProperties.class,
    WebSocketProperties.class,
    RealtimeProperties.class,
    AiProperties.class,
    MessagingProperties.class
})
public class AppConfig {

  @Bean(name = "aiExecutor")
  public Executor aiExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ai-worker-");
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(500);
    executor.initialize();
    return executor;
  }
}
