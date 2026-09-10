package com.fastvue.module.analytics.api;

import java.util.List;

/**
 * 数据分析概览。
 *
 * <p>对应前端契约 {@code AnalyticsOverview}：stats / topPages / trend。</p>
 */
public record AnalyticsOverview(
        List<Stat> stats,
        List<TopPage> topPages,
        List<TrendPoint> trend) {

    /** 统计指标卡片 */
    public record Stat(String color, String suffix, String title, Object value) {
    }

    /** 热门页面 */
    public record TopPage(String page, String title, int visits, String avgTime) {
    }

    /** 趋势序列中的单日数据点 */
    public record TrendPoint(String date, int visits, int users, int orders, int revenue) {
    }
}
