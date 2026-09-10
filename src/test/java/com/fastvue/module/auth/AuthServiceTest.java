package com.fastvue.module.auth;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.auth.api.LoginRequest;
import com.fastvue.module.auth.api.RefreshTokenRequest;
import com.fastvue.module.auth.api.TokenResponse;
import com.fastvue.module.auth.service.AuthService;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.module.tenant.service.TenantService;
import com.fastvue.module.tenant.persistence.TenantEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试（Mockito，无需数据库 / Redis）。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TenantService tenantService;

    @InjectMocks
    private AuthService authService;

    private final UserEntity admin = new UserEntity();

    @BeforeEach
    void setUp() {
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("hashed");
        admin.setNickname("Administrator");
    }

    @Test
    @DisplayName("登录成功：签发 Access + Refresh Token")
    void loginSuccess() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(1L);
        when(tenantService.requireActive("default")).thenReturn(tenant);
        when(userMapper.selectOne(any())).thenReturn(admin);
        when(jwtTokenProvider.generateAccessToken(1L, "admin")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L, "admin"))
                .thenReturn(new JwtTokenProvider.TokenPair("jti-1", "refresh-token"));
        when(jwtTokenProvider.accessTokenTtlSeconds()).thenReturn(7200L);
        when(jwtTokenProvider.refreshTokenTtlSeconds()).thenReturn(604800L);

        TokenResponse response = authService.login(new LoginRequest("admin", "password"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(7200L);
        verify(refreshTokenStore).save(anyLong(), anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("登录失败：用户名或密码错误")
    void loginFailure() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(1L);
        when(tenantService.requireActive("default")).thenReturn(tenant);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }

    @Test
    @DisplayName("刷新令牌：无效 token 抛出异常")
    void refreshInvalidToken() {
        when(jwtTokenProvider.parse("bad-token"))
                .thenThrow(new io.jsonwebtoken.JwtException("invalid"));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("bad-token")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("刷新令牌：禁用用户不能换取新令牌")
    void refreshDisabledUser() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parse("refresh-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("username", String.class)).thenReturn("admin");
        when(refreshTokenStore.consume(1L, "jti-1", "admin")).thenReturn(true);
        admin.setStatus("disabled");
        when(userMapper.selectById(1L)).thenReturn(admin);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("refresh-token")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString());
    }
}
