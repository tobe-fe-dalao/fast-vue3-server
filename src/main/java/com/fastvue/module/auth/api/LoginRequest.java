package com.fastvue.module.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        String tenantCode) {

    public LoginRequest(String username, String password) {
        this(username, password, "default");
    }
}
