package com.fastvue.module.role.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 角色视图对象。
 */
public record RoleVO(
        Long id,
        String code,
        String name,
        String description,
        List<String> permissions,
        List<Long> menuIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
