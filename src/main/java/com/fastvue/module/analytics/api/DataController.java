package com.fastvue.module.analytics.api;

import com.fastvue.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据中心接口（内存确定性数据，无需数据库）。
 *
 * <p>经营数据需要 {@code data:view} 权限。</p>
 */
@Tag(name = "数据中心")
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Operation(summary = "数据中心概览")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('data:view')")
    public ApiResponse<DataOverview> overview() {
        LocalDate today = LocalDate.now();

        List<DataOverview.Stat> stats = List.of(
                new DataOverview.Stat("#1890ff", null, "人", "总用户数", 12480L),
                new DataOverview.Stat("#52c41a", null, "单", "今日订单", 156L),
                new DataOverview.Stat("#faad14", "¥", "万", "月度营收", 28.46),
                new DataOverview.Stat("#f5222d", null, "万", "访问量", 12.48));

        List<DataOverview.Recent> recent = List.of(
                new DataOverview.Recent(today.format(DATE_FMT), "订单支付", "¥1,280.00", "成功"),
                new DataOverview.Recent(today.minusDays(1).format(DATE_FMT), "会员续费", "¥399.00", "成功"),
                new DataOverview.Recent(today.minusDays(2).format(DATE_FMT), "退款", "¥199.00", "已处理"),
                new DataOverview.Recent(today.minusDays(3).format(DATE_FMT), "订单支付", "¥2,560.00", "成功"),
                new DataOverview.Recent(today.minusDays(4).format(DATE_FMT), "提现", "¥880.00", "处理中"),
                new DataOverview.Recent(today.minusDays(5).format(DATE_FMT), "订单支付", "¥640.00", "成功"));

        return ApiResponse.success(new DataOverview(recent, stats));
    }
}
