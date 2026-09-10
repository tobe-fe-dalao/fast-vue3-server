package com.fastvue.module.user.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户视图对象（不暴露密码）。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String status,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
