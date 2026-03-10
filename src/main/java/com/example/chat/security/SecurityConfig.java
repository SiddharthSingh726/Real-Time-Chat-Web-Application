package com.example.chat.security;

import com.example.chat.auth.LocalAuthService;
import com.example.chat.config.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
                                          SecurityProperties securityProperties,
                                          LocalAuthService localAuthService) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/error").permitAll()
            .requestMatchers("/styles.css", "/app.js").permitAll()
            .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/presence/**").authenticated()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll());

    if (securityProperties.isDevMode()) {
      http.addFilterBefore(
          new DevAuthenticationFilter(localAuthService, securityProperties.isAllowHeaderImpersonation()),
          UsernamePasswordAuthenticationFilter.class);
    } else {
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    }

    return http.build();
  }

  @Bean
  org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
  }
}
