package com.fastvue.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.*;

class RateLimitFilterTest {
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final FilterChain chain = mock(FilterChain.class);
    final RateLimitFilter filter = new RateLimitFilter(redis, new ObjectMapper());

    @ParameterizedTest
    @CsvSource({
        "/api/v1/auth/login,10,login",
        "/api/v1/auth/register,5,register",
        "/api/v1/auth/refresh,30,refresh",
        "/api/v1/public/articles,120,public",
        "/api/v1/users,300,api"
    })
    void excessiveRequestsStopBeforeAuthentication(String path, long limit, String bucket)
            throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList()))
                .thenReturn(limit + 1);
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", path), response, chain);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("\"code\":429");
        verify(redis)
                .execute(
                        org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                        eq(java.util.List.of("rate:" + bucket + ":127.0.0.1")));
        verifyNoInteractions(chain);
    }

    @Test
    void allowedRequestsContinue() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList()))
                .thenReturn(300L);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void preflightAndHealthDoNotAccessRedis() throws Exception {
        for (var request :
                java.util.List.of(
                        new MockHttpServletRequest("OPTIONS", "/api/v1/users"),
                        new MockHttpServletRequest("GET", "/actuator/health"))) {
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
        }
        verifyNoInteractions(redis);
    }

    @Test
    void unavailableRedisFailsClosedWithoutLeakingDetails() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList()))
                .thenThrow(new IllegalStateException("private connection details"));
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"), response, chain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).doesNotContain("private");
        verifyNoInteractions(chain);
    }

    @Test
    void missingRedisResultAlsoFailsClosed() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"), response, chain);
        assertThat(response.getStatus()).isEqualTo(503);
        verifyNoInteractions(chain);
    }
}
