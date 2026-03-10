package com.example.chat.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.chat.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiClient implements AiClient {

  private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

  private final AiProperties properties;
  private final RestClient restClient;
  private final Counter requestCounter;
  private final Counter failureCounter;

  public OpenAiClient(AiProperties properties, MeterRegistry meterRegistry) {
    this.properties = properties;
    this.restClient = RestClient.builder()
        .baseUrl(properties.getOpenai().getBaseUrl())
        .build();
    this.requestCounter = Counter.builder("chat.ai.requests").register(meterRegistry);
    this.failureCounter = Counter.builder("chat.ai.failures").register(meterRegistry);
  }

  @Override
  public Optional<String> generateReply(UUID conversationId, String prompt) {
    requestCounter.increment();
    try {
      Map<String, Object> payload = Map.of(
          "model", properties.getOpenai().getModel(),
          "store", false,
          "input", List.of(
              Map.of(
                  "role", "system",
                  "content", List.of(Map.of("type", "input_text", "text", properties.getOpenai().getSystemPrompt()))),
              Map.of(
                  "role", "user",
                  "content", List.of(Map.of("type", "input_text", "text", prompt)))));

      JsonNode response = restClient.post()
          .uri("/responses")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOpenai().getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(JsonNode.class);

      String output = extractText(response);
      return StringUtils.hasText(output) ? Optional.of(output.trim()) : Optional.empty();
    } catch (Exception ex) {
      failureCounter.increment();
      log.warn("AI reply generation failed for conversation {}: {}", conversationId, ex.getMessage());
      return Optional.empty();
    }
  }

  private String extractText(JsonNode response) {
    if (response == null) {
      return null;
    }

    JsonNode outputText = response.get("output_text");
    if (outputText != null && outputText.isTextual()) {
      return outputText.asText();
    }

    JsonNode outputArray = response.get("output");
    if (outputArray == null || !outputArray.isArray()) {
      return null;
    }

    StringBuilder builder = new StringBuilder();
    for (JsonNode output : outputArray) {
      JsonNode content = output.get("content");
      if (content == null || !content.isArray()) {
        continue;
      }
      for (JsonNode part : content) {
        JsonNode text = part.get("text");
        if (text != null && text.isTextual()) {
          if (!builder.isEmpty()) {
            builder.append('\n');
          }
          builder.append(text.asText());
        }
      }
    }

    return builder.toString();
  }
}
