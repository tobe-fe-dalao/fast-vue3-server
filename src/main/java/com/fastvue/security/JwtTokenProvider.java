package com.fastvue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import com.fastvue.infrastructure.tenant.TenantContext;

/**
 * JWT 生成与解析工具。
 *
 * <p>Access Token 与 Refresh Token 使用相同签名密钥、不同过期时间与用途声明区分。
 * Refresh Token 额外携带 {@code jti}，用于在 Redis 中标识与轮换。</p>
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl}") long accessTokenTtlSeconds,
            @Value("${app.jwt.refresh-token-ttl}") long refreshTokenTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    /**
     * 生成 Access Token，载荷中包含用户 id 与用户名。
     */
    public String generateAccessToken(Long userId, String username) {
        return buildToken(userId, username, "access", null, accessTokenTtlSeconds);
    }

    /**
     * 生成 Refresh Token，返回其 jti 与 token 字符串。
     *
     * @return {@link TokenPair}，其中 {@code jti} 用于 Redis 存储标识
     */
    public TokenPair generateRefreshToken(Long userId, String username) {
        String jti = UUID.randomUUID().toString();
        String token = buildToken(userId, username, "refresh", jti, refreshTokenTtlSeconds);
        return new TokenPair(jti, token);
    }

    private String buildToken(Long userId, String username, String type, String jti, long ttlSeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("tenantId", TenantContext.tenantId())
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)));
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(key).compact();
    }

    /**
     * 解析并校验签名与过期时间；无效时抛出 {@link io.jsonwebtoken.JwtException}。
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long refreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    /**
     * Refresh Token 生成结果。
     */
    public record TokenPair(String jti, String token) {
    }
}
