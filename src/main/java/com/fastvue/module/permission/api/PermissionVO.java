package com.fastvue.module.permission.api;

import java.time.OffsetDateTime;

/**
 * 权限视图对象。
 */
public record PermissionVO(
        Long id,
        String code,
        String name,
        String description,
        OffsetDateTime createdAt) {
}
