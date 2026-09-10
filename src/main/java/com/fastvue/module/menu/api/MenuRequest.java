package com.fastvue.module.menu.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建 / 更新菜单请求。
 */
public record MenuRequest(
        Long parentId,

        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 64, message = "菜单名称长度不能超过 64")
        String name,

        @Size(max = 255, message = "路径长度不能超过 255")
        String path,

        @Size(max = 255, message = "组件路径长度不能超过 255")
        String component,

        @Size(max = 64, message = "图标长度不能超过 64")
        String icon,

        Integer sort,

        Boolean visible,

        @Size(max = 128, message = "权限标识长度不能超过 128")
        String permission,

        @NotBlank(message = "菜单类型不能为空")
        @Pattern(regexp = "directory|menu|button", message = "菜单类型只能是 directory、menu 或 button")
        String type) {

    public MenuRequest {
        parentId = parentId == null ? 0L : parentId;
        sort = sort == null ? 0 : sort;
        visible = visible == null ? true : visible;
    }
}
