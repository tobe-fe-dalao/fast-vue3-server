package com.fastvue.module.role;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 更新角色请求。
 */
public record UpdateRoleRequest(
        @Size(max = 64, message = "角色名称长度不能超过 64")
        String name,

        @Size(max = 255, message = "角色描述长度不能超过 255")
        String description,

        List<@Positive(message = "权限 ID 必须大于 0") Long> permissionIds,

        List<@Positive(message = "菜单 ID 必须大于 0") Long> menuIds) {
}
