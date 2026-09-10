package com.fastvue.module.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken 不能为空") String refreshToken) {
}
