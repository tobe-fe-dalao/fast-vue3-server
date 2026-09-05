package com.fastvue.module.auth;

/**
 * 登录 / 刷新成功后的令牌响应。
 *
 * @param accessToken  Access Token
 * @param refreshToken Refresh Token
 * @param expiresIn    Access Token 有效期（秒）
 */
public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
