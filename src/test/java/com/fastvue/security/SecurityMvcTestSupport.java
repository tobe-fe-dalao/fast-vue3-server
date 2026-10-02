package com.fastvue.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** MVC slices test authorization without Redis; production-chain ordering is covered with real Redis. */
public abstract class SecurityMvcTestSupport {
    @MockitoBean RateLimitFilter rateLimitFilter;

    @BeforeEach
    void allowRequestsThroughLimiter() throws Exception {
        doAnswer(
                        invocation -> {
                            ((FilterChain) invocation.getArgument(2))
                                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
                            return null;
                        })
                .when(rateLimitFilter)
                .doFilter(any(), any(), any());
    }
}
