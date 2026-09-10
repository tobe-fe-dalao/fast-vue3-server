package com.fastvue.module.content.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 文章分页查询请求。
 */
public record ArticleQueryRequest(
        String keyword,
        String status,
        Long categoryId,
        @Min(value = 1, message = "page 不能小于 1") Long page,
        @Min(value = 1, message = "pageSize 不能小于 1")
        @Max(value = 100, message = "pageSize 不能大于 100") Long pageSize) {

    public ArticleQueryRequest {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
    }
}
