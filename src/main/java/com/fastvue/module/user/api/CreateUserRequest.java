package com.fastvue.module.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求。
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过 64")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度须在 6-100 之间")
        String password,

        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone) {
}
