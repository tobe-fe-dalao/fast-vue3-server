package com.fastvue.security;

import static org.assertj.core.api.Assertions.*;

import com.fastvue.config.JwtProperties;
import com.fastvue.infrastructure.tenant.TenantContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.*;

class JwtTokenProviderTest {
    static final String SECRET = "test-key-with-more-than-thirty-two-bytes";
    final JwtTokenProvider tokens =
            new JwtTokenProvider(new JwtProperties(SECRET, 900, 86400, "issuer", "audience"));

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void tokensRequireIssuerAudienceExpiryAndTenant() {
        TenantContext.set(7L, false);
        assertThat(
                        tokens.parse(tokens.generateAccessToken(9L, "alice"))
                                .get("tenantId", Long.class))
                .isEqualTo(7L);
        assertThat(tokens.parse(tokens.generateRefreshToken(9L, "alice").token()).get("type"))
                .isEqualTo("refresh");
        for (String issuer : new String[] {"wrong", "issuer"}) {
            String bad =
                    Jwts.builder()
                            .subject("9")
                            .issuer(issuer)
                            .audience()
                            .add("wrong")
                            .and()
                            .expiration(Date.from(Instant.now().plusSeconds(60)))
                            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                            .compact();
            assertThatThrownBy(() -> tokens.parse(bad)).isInstanceOf(JwtException.class);
        }
        String noExpiry =
                Jwts.builder()
                        .subject("9")
                        .issuer("issuer")
                        .audience()
                        .add("audience")
                        .and()
                        .claim("tenantId", 7L)
                        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                        .compact();
        assertThatThrownBy(() -> tokens.parse(noExpiry)).isInstanceOf(JwtException.class);
    }
}
