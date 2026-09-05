package com.fastvue.module.user;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新用户请求。所有字段均可选，仅更新传入的字段。
 */
public record UpdateUserRequest(
        @Size(min = 6, max = 100, message = "密码长度须在 6-100 之间")
        String password,

        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone,

        String status,

        List<Long> roleIds) {
}
