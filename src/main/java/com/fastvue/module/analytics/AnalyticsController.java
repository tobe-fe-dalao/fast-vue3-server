package com.fastvue.module.analytics;

import com.fastvue.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据分析接口（内存确定性数据，无需数据库）。
 *
 * <p>经营分析数据需要 {@code analytics:view} 权限。</p>
 */
@Tag(name = "数据分析")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Operation(summary = "数据分析概览")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<AnalyticsOverview> overview(@RequestParam(defaultValue = "7") int days) {
        int d = Math.max(1, Math.min(90, days));
        LocalDate today = LocalDate.now();

        List<AnalyticsOverview.Stat> stats = List.of(
                new AnalyticsOverview.Stat("#1890ff", "次", "总访问量", 86540L + (long) d * 1200),
                new AnalyticsOverview.Stat("#52c41a", "人", "总用户数", 12480L),
                new AnalyticsOverview.Stat("#faad14", "元", "总营收", 284600L + (long) d * 3500),
                new AnalyticsOverview.Stat("#f5222d", "%", "系统正常率", 99.9));

        List<AnalyticsOverview.TrendPoint> trend = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            int idx = d - 1 - i;
            trend.add(new AnalyticsOverview.TrendPoint(
                    day.format(DATE_FMT),
                    1200 + (idx * 137) % 520,
                    320 + (idx * 53) % 210,
                    80 + (idx * 29) % 60,
                    5000 + (idx * 911) % 3200));
        }

        List<AnalyticsOverview.TopPage> topPages = List.of(
                new AnalyticsOverview.TopPage("/dashboard", "仪表盘", 4520, "2m 30s"),
                new AnalyticsOverview.TopPage("/analytics", "数据分析", 3860, "3m 10s"),
                new AnalyticsOverview.TopPage("/content/article", "内容管理", 2980, "1m 50s"),
                new AnalyticsOverview.TopPage("/system/user", "用户管理", 3180, "2m 05s"),
                new AnalyticsOverview.TopPage("/portal/home", "门户首页", 2240, "4m 12s"));

        return ApiResponse.success(new AnalyticsOverview(stats, topPages, trend));
    }
}
