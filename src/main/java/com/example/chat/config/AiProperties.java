package com.example.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

  private boolean enabled;
  private String triggerPrefix = "@ai";
  private String provider = "openai";
  private OpenAi openai = new OpenAi();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getTriggerPrefix() {
    return triggerPrefix;
  }

  public void setTriggerPrefix(String triggerPrefix) {
    this.triggerPrefix = triggerPrefix;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public OpenAi getOpenai() {
    return openai;
  }

  public void setOpenai(OpenAi openai) {
    this.openai = openai;
  }

  public static class OpenAi {

    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4.1-mini";
    private String apiKey = "";
    private int timeoutSeconds = 30;
    private String systemPrompt = "You are a concise assistant in team chat.";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public int getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }

    public String getSystemPrompt() {
      return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
      this.systemPrompt = systemPrompt;
    }
  }
}
