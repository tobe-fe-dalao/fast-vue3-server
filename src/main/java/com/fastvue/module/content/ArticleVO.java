package com.fastvue.module.content;

import java.util.List;

/**
 * 文章视图对象。
 */
public record ArticleVO(
        Long id,
        String author,
        String category,
        Long categoryId,
        List<String> content,
        String cover,
        String date,
        String status,
        String summary,
        List<String> tags,
        String title) {
}
