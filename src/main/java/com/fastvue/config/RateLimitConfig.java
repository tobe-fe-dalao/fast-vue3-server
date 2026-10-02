package com.fastvue.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.security.RateLimitFilter;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfig {
    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<RateLimitFilter>
            rateLimitRegistration(RateLimitFilter filter) {
        var registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    RateLimitFilter rateLimitFilter(StringRedisTemplate redis, ObjectMapper mapper) {
        return new RateLimitFilter(redis, mapper);
    }
}
