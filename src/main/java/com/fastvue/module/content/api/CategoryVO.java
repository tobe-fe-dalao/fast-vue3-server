package com.fastvue.module.content.api;

/**
 * 分类视图对象。
 */
public record CategoryVO(
        Long id,
        String description,
        String name,
        String slug,
        String status) {
}
