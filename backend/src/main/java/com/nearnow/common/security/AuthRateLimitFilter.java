package com.nearnow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final StringRedisTemplate redis;

    public AuthRateLimitFilter(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("/api/auth/login".equals(path) || "/api/auth/register".equals(path) || "/api/auth/refresh".equals(path)
                || "/api/auth/forgot-password".equals(path) || "/api/auth/reset-password".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String key = "rate:auth:" + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) redis.expire(key, WINDOW);
        if (count != null && count > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many authentication attempts. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
