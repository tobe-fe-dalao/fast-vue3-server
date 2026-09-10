package com.fastvue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = request.getRequestURI().equals("/api/v1/auth/login") ? 10 : 120;
        if ("GET".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response); return;
        }
        String key = "rate:" + request.getRemoteAddr() + ":" + request.getRequestURI();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) redis.expire(key, Duration.ofMinutes(1));
            if (count != null && count > limit) {
                response.setStatus(429); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.RATE_LIMITED));
                return;
            }
        } catch (Exception ex) {
            log.warn("限流存储不可用，当前请求降级放行: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }
}
