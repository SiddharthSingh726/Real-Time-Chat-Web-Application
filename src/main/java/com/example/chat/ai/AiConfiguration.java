package com.example.chat.ai;

import com.example.chat.config.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class AiConfiguration {

  @Bean
  @Primary
  public AiClient aiClient(AiProperties properties, OpenAiClient openAiClient, NoopAiClient noopAiClient) {
    if (!properties.isEnabled()) {
      return noopAiClient;
    }

    if ("openai".equalsIgnoreCase(properties.getProvider())
        && StringUtils.hasText(properties.getOpenai().getApiKey())) {
      return openAiClient;
    }

    return noopAiClient;
  }
}
