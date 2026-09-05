package com.fastvue.module.auth;

import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import com.fastvue.module.user.UserEntity;
import com.fastvue.module.user.UserMapper;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
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
}
