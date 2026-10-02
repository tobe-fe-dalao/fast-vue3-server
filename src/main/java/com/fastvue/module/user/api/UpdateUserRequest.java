package com.fastvue.module.user.api;

import com.fastvue.common.validation.BcryptPassword;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 更新用户请求。所有字段均可选，仅更新传入的字段。
 */
public record UpdateUserRequest(
        @Size(min = 6, max = 72, message = "密码长度须在 6-72 之间") @BcryptPassword String password,
        @Size(max = 64, message = "昵称长度不能超过 64") String nickname,
        @Size(max = 128, message = "邮箱长度不能超过 128") String email,
        @Size(max = 32, message = "手机号长度不能超过 32") String phone,
        @Pattern(regexp = "active|disabled", message = "用户状态只能是 active 或 disabled") String status,
        @Size(max = 1000)
                List<
                                @jakarta.validation.constraints.NotNull
                                @Positive(message = "角色 ID 必须大于 0") Long>
                        roleIds) {}
