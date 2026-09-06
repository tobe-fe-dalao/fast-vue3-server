package com.fastvue.module.analytics;

import java.util.List;

/**
 * 数据中心概览。
 *
 * <p>对应前端契约 {@code DataOverview}：stats / recent。</p>
 */
public record DataOverview(
        List<Recent> recent,
        List<Stat> stats) {

    /** 统计指标卡片 */
    public record Stat(String border, String prefix, String suffix, String title, Object value) {
    }

    /** 最近交易流水 */
    public record Recent(String date, String type, String amount, String status) {
    }
}
