package com.fastvue.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 统一分页响应。
 *
 * @param items    当前页数据
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 * @param total    总条数
 */
public record PageResponse<T>(List<T> items, long page, long pageSize, long total) {

    public static <T> PageResponse<T> of(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    public static <T> PageResponse<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageResponse<>(items, page, pageSize, total);
    }
}
