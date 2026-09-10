package com.fastvue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/** Short-lived RBAC snapshot cache with a global version for deterministic invalidation. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCache {
    private static final String VERSION_KEY = "rbac:version";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public Snapshot get(Long userId, Supplier<Snapshot> loader) {
        String key = null;
        try {
            String version = redis.opsForValue().get(VERSION_KEY);
            if (version == null) version = "0";
            key = "rbac:" + version + ":user:" + userId;
            String cached = redis.opsForValue().get(key);
            if (cached != null) return objectMapper.readValue(cached, Snapshot.class);
        } catch (Exception ex) {
            log.warn("权限缓存不可用，回退数据库查询: {}", ex.getMessage());
        }
        Snapshot loaded = loader.get();
        if (key != null) {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(loaded), Duration.ofMinutes(5));
            } catch (Exception ex) {
                log.warn("权限缓存写入失败: {}", ex.getMessage());
            }
        }
        return loaded;
    }

    public void invalidateAll() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    incrementVersion();
                }
            });
            return;
        }
        incrementVersion();
    }

    private void incrementVersion() {
        try {
            redis.opsForValue().increment(VERSION_KEY);
        } catch (Exception ex) {
            log.warn("权限缓存版本更新失败: {}", ex.getMessage());
        }
    }

    public record Snapshot(List<String> roles, List<String> permissions) {}
}
