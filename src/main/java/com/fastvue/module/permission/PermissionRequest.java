package com.fastvue.module.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建 / 更新权限请求。
 */
public record PermissionRequest(
        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码长度不能超过 128")
        String code,

        @NotBlank(message = "权限名称不能为空")
        @Size(max = 128, message = "权限名称长度不能超过 128")
        String name,

        @Size(max = 255, message = "权限描述长度不能超过 255")
        String description) {
}
