package com.fastvue.infrastructure;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.idempotency.IdempotencyService;
import com.fastvue.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyTest {
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void duplicateKeyIsRejected() {
        TenantContext.set(1L, false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("idempotency:1:project-create:same", "processing", Duration.ofHours(24)))
                .thenReturn(false);
        IdempotencyService service = new IdempotencyService(redis);

        assertThatThrownBy(() -> service.execute("project-create", "same", () -> "never"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test void completedCommandStillReturnsSuccessWhenRedisCompletionMarkFails() {
        TenantContext.set(1L, false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("idempotency:1:project-create:key", "processing", Duration.ofHours(24)))
                .thenReturn(true);
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(values).set("idempotency:1:project-create:key", "completed", Duration.ofHours(24));

        assertThat(new IdempotencyService(redis).execute("project-create", "key", () -> "created"))
                .isEqualTo("created");
        verify(redis, never()).delete("idempotency:1:project-create:key");
    }
}
