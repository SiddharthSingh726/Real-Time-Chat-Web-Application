package com.example.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

  public enum Mode {
    DEV,
    OAUTH2
  }

  private Mode mode = Mode.DEV;
  private boolean allowHeaderImpersonation = true;

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public boolean isDevMode() {
    return mode == Mode.DEV;
  }

  public boolean isAllowHeaderImpersonation() {
    return allowHeaderImpersonation;
  }

  public void setAllowHeaderImpersonation(boolean allowHeaderImpersonation) {
    this.allowHeaderImpersonation = allowHeaderImpersonation;
  }
}
