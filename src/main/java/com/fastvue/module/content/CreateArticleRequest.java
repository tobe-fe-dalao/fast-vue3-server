package com.fastvue.module.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建文章请求。
 */
public record CreateArticleRequest(
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 255, message = "标题长度不能超过 255")
        String title,

        @Size(max = 64, message = "作者长度不能超过 64")
        String author,

        Long categoryId,

        List<String> content,

        @Size(max = 255, message = "封面地址长度不能超过 255")
        String cover,

        String status,

        @Size(max = 512, message = "摘要长度不能超过 512")
        String summary,

        List<String> tags) {
}
