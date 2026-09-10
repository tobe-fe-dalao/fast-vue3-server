package com.fastvue.module.auth.service;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.auth.api.LoginRequest;
import com.fastvue.module.auth.api.MeResponse;
import com.fastvue.module.auth.api.RefreshTokenRequest;
import com.fastvue.module.auth.api.TokenResponse;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.LoginUser;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.security.SecurityUtils;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.service.TenantService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 认证业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserMapper userMapper;
    private final TenantService tenantService;

    /**
     * 登录：校验凭证，签发 Access + Refresh Token。
     */
    public TokenResponse login(LoginRequest request) {
        TenantEntity tenant = tenantService.requireActive(request.tenantCode());
        return TenantContext.runAs(tenant.getId(), false, () -> doLogin(request));
    }

    private TokenResponse doLogin(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("登录失败: 用户名 {} 凭证无效或账号不可用", request.username());
            throw new BusinessException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        UserEntity user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.username()));
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        JwtTokenProvider.TokenPair refreshPair =
                jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        refreshTokenStore.save(
                user.getId(),
                refreshPair.jti(),
                user.getUsername(),
                Duration.ofSeconds(jwtTokenProvider.refreshTokenTtlSeconds()));

        log.info("用户 {} 登录成功", user.getUsername());
        return new TokenResponse(accessToken, refreshPair.token(), jwtTokenProvider.accessTokenTtlSeconds());
    }

    /**
     * 刷新令牌：校验 Refresh Token，轮换新旧 token。
     */
    public TokenResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String jti = claims.getId();
        String username = claims.get("username", String.class);
        Long tenantId = claims.get("tenantId", Long.class);
        if (tenantId == null) {
            tenantId = 1L;
        }

        tenantService.requireActive(tenantId);
        Long effectiveTenantId = tenantId;
        return TenantContext.runAs(effectiveTenantId, false,
                () -> rotateRefreshToken(userId, jti, username));
    }

    private TokenResponse rotateRefreshToken(Long userId, String jti, String username) {

        // 原子消费旧 Token：同一枚 Refresh Token 只能轮换一次。
        if (jti == null || username == null || !refreshTokenStore.consume(userId, jti, username)) {
            log.warn("刷新令牌已被撤销或不存在: userId={}", userId);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null || !"active".equals(user.getStatus()) || !username.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 轮换：旧 Refresh Token 已被消费，签发新的一对。
        String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername());
        JwtTokenProvider.TokenPair newRefreshPair =
                jwtTokenProvider.generateRefreshToken(userId, user.getUsername());
        refreshTokenStore.save(
                userId,
                newRefreshPair.jti(),
                user.getUsername(),
                Duration.ofSeconds(jwtTokenProvider.refreshTokenTtlSeconds()));

        return new TokenResponse(accessToken, newRefreshPair.token(), jwtTokenProvider.accessTokenTtlSeconds());
    }

    /**
     * 登出：撤销当前用户提供的 Refresh Token。
     */
    public void logout(RefreshTokenRequest request) {
        try {
            Claims claims = jwtTokenProvider.parse(request.refreshToken());
            if ("refresh".equals(claims.get("type"))) {
                Long userId = Long.valueOf(claims.getSubject());
                refreshTokenStore.delete(userId, claims.getId());
                log.info("用户 {} 登出，Refresh Token 已撤销", userId);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // 登出幂等：token 无效时无需处理
        }
    }

    /**
     * 获取当前登录用户信息（含角色与权限）。
     */
    public MeResponse me() {
        LoginUser loginUser = SecurityUtils.currentUser();
        UserEntity user = userMapper.selectById(loginUser.id());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        List<String> permissions = userMapper.selectPermissionCodes(user.getId());
        return new MeResponse(user.getId(), user.getTenantId(), user.getUsername(), user.getNickname(),
                loginUser.roles(), permissions);
    }
}
