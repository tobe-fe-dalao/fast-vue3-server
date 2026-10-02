package com.fastvue.module.auth.api;

import com.fastvue.common.validation.BcryptPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") @BcryptPassword String password,
        String tenantCode) {

    public LoginRequest(String username, @BcryptPassword String password) {
        this(username, password, "default");
    }
}
