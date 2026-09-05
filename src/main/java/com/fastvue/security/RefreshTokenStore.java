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
     * 校验 Refresh Token 是否存在且属于指定用户；存在时返回 true。
     */
    public boolean exists(Long userId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(userId, jti)));
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
