package com.fastvue.module.site.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create-comment request. */
public record CreateBlogCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
        String content) {
}
