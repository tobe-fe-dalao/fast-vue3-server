package com.fastvue.module.role.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建角色请求。
 */
public record CreateRoleRequest(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 64, message = "角色编码长度不能超过 64")
        String code,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称长度不能超过 64")
        String name,

        @Size(max = 255, message = "角色描述长度不能超过 255")
        String description,

        List<@Positive(message = "权限 ID 必须大于 0") Long> permissionIds,

        List<@Positive(message = "菜单 ID 必须大于 0") Long> menuIds) {
}
