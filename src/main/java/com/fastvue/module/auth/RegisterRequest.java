package com.fastvue.module.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 站点公开注册请求。 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过 64")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度须在 6-100 之间")
        String password) {
}
