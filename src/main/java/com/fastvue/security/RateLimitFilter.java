package com.fastvue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private static final DefaultRedisScript<Long> COUNT =
            new DefaultRedisScript<>(
                    """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 or redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], 60) end
            return count
            """,
                    Long.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String bucket =
                path.equals("/api/v1/auth/login")
                        ? "login"
                        : path.equals("/api/v1/auth/register")
                                ? "register"
                                : path.equals("/api/v1/auth/refresh")
                                        ? "refresh"
                                        : path.startsWith("/api/v1/public/") ? "public" : "api";
        int limit =
                switch (bucket) {
                    case "login" -> 10;
                    case "register" -> 5;
                    case "refresh" -> 30;
                    case "public" -> 120;
                    default -> 300;
                };
        Long count;
        try {
            count = redis.execute(COUNT, List.of("rate:" + bucket + ":" + request.getRemoteAddr()));
            if (count == null) throw new IllegalStateException("No rate-limit result");
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable ({})", ex.getClass().getSimpleName());
            write(response, ErrorCode.SERVICE_UNAVAILABLE);
            return;
        }
        if (count > limit) {
            write(response, ErrorCode.RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    private void write(HttpServletResponse response, ErrorCode error) throws IOException {
        response.setStatus(error.code());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
    }
}
