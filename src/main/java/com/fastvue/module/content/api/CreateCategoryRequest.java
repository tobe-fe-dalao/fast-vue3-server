package com.fastvue.module.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建分类请求。
 */
public record CreateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称长度不能超过 64")
        String name,

        @NotBlank(message = "分类标识不能为空")
        @Size(max = 64, message = "分类标识长度不能超过 64")
        String slug,

        @Size(max = 255, message = "分类描述长度不能超过 255")
        String description,

        String status) {
}
