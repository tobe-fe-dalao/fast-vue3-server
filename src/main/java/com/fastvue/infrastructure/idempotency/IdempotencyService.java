package com.fastvue.infrastructure.idempotency;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/** Redis-backed request claim used only for non-repeatable business commands. */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;

    public <T> T execute(String operation, String idempotencyKey, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key 必填且不能超过 128 字符");
        }
        String redisKey = "idempotency:" + TenantContext.tenantId() + ":" + operation + ":" + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "processing", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        T result;
        try {
            result = action.get();
        } catch (RuntimeException ex) {
            try {
                redisTemplate.delete(redisKey);
            } catch (RuntimeException cleanupFailure) {
                log.warn("幂等声明清理失败 key={}: {}", redisKey, cleanupFailure.getMessage());
            }
            throw ex;
        }
        try {
            redisTemplate.opsForValue().set(redisKey, "completed", Duration.ofHours(24));
        } catch (RuntimeException ex) {
            // The command may have committed already; a Redis write failure cannot turn it
            // into a safe-to-retry failure response.
            log.warn("业务已完成但幂等结果标记失败 key={}: {}", redisKey, ex.getMessage());
        }
        return result;
    }
}
