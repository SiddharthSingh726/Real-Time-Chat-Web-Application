package com.example.chat.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.chat.auth.LocalAuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class DevAuthenticationFilter extends OncePerRequestFilter {

  public static final String USER_HEADER = "X-User-Id";
  private final LocalAuthService localAuthService;
  private final boolean allowHeaderImpersonation;

  public DevAuthenticationFilter(LocalAuthService localAuthService,
                                 boolean allowHeaderImpersonation) {
    this.localAuthService = localAuthService;
    this.allowHeaderImpersonation = allowHeaderImpersonation;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String userId = resolveUserId(request);
      if (!StringUtils.hasText(userId)) {
        userId = request.getParameter("userId");
      }

      if (StringUtils.hasText(userId)) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveUserId(HttpServletRequest request) {
    String authToken = request.getHeader(LocalAuthService.AUTH_TOKEN_HEADER);
    if (StringUtils.hasText(authToken)) {
      try {
        return localAuthService.resolveUserId(authToken);
      } catch (Exception ignored) {
        return null;
      }
    }
    if (allowHeaderImpersonation) {
      return request.getHeader(USER_HEADER);
    }
    return null;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || path.startsWith("/static")
        || path.startsWith("/css") || path.startsWith("/js")
        || path.startsWith("/api/auth")
        || !path.startsWith("/api");
  }
}
