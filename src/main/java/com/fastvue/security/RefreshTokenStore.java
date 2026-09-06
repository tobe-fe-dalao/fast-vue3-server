package com.fastvue.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 的 Redis 存储。
 *
 * <p>键格式 {@code refresh-token:{userId}:{jti}}，值为用户名。Logout 时删除对应键使其失效。</p>
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 保存 Refresh Token，返回用于标识该 token 的 key（含 userId 与 jti）。
     */
    public void save(Long userId, String jti, String username, Duration ttl) {
        redisTemplate.opsForValue().set(buildKey(userId, jti), username, ttl);
    }

    /**
     * 原子消费 Refresh Token。只有 Redis 中存在且用户名一致时才返回 true。
     *
     * <p>使用 get-and-delete 避免「先查询、后删除」导致的并发重放窗口。</p>
     */
    public boolean consume(Long userId, String jti, String expectedUsername) {
        String storedUsername = redisTemplate.opsForValue().getAndDelete(buildKey(userId, jti));
        return expectedUsername.equals(storedUsername);
    }

    /**
     * 删除 Refresh Token（Logout 或刷新后轮换）。
     */
    public void delete(Long userId, String jti) {
        redisTemplate.delete(buildKey(userId, jti));
    }

    private String buildKey(Long userId, String jti) {
        return KEY_PREFIX + userId + ":" + jti;
    }
}
