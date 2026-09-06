package com.fastvue.module.content;

import jakarta.validation.constraints.Size;

/**
 * 更新分类请求（全字段可选）。
 */
public record UpdateCategoryRequest(
        @Size(max = 64, message = "分类名称长度不能超过 64")
        String name,

        @Size(max = 64, message = "分类标识长度不能超过 64")
        String slug,

        @Size(max = 255, message = "分类描述长度不能超过 255")
        String description,

        String status) {
}
