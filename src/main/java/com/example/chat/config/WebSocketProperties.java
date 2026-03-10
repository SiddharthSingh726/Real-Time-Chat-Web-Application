package com.example.chat.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {

  private String endpoint = "/ws";
  private String applicationPrefix = "/app";
  private String topicPrefix = "/topic";
  private String queuePrefix = "/queue";
  private List<String> allowedOriginPatterns = new ArrayList<>();

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getApplicationPrefix() {
    return applicationPrefix;
  }

  public void setApplicationPrefix(String applicationPrefix) {
    this.applicationPrefix = applicationPrefix;
  }

  public String getTopicPrefix() {
    return topicPrefix;
  }

  public void setTopicPrefix(String topicPrefix) {
    this.topicPrefix = topicPrefix;
  }

  public String getQueuePrefix() {
    return queuePrefix;
  }

  public void setQueuePrefix(String queuePrefix) {
    this.queuePrefix = queuePrefix;
  }

  public List<String> getAllowedOriginPatterns() {
    return allowedOriginPatterns;
  }

  public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = allowedOriginPatterns;
  }
}
