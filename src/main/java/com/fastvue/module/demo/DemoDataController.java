package com.fastvue.module.demo;

import com.fastvue.common.ApiResponse;
import com.fastvue.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 为管理端演示页面提供稳定、可替换的内存数据接口（门户相关接口已迁移至 portal 模块）。
 *
 * <p>这些端点先使用确定性的内存数据，避免页面直接依赖本地 mock。后续接入数据库时，
 * 保持响应契约不变即可替换实现。</p>
 */
@Tag(name = "演示数据")
@RestController
@RequestMapping("/api/v1")
public class DemoDataController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<Map<String, Object>> CONFIGS = List.of(
            item("id", 1L, "name", "主框架侧边栏主题", "key", "sys.index.sideTheme", "value", "theme-dark",
                    "type", "built-in", "remark", "侧边栏主题（dark / light）", "createdAt", "2024-01-05 09:00:00"),
            item("id", 2L, "name", "账号自助注册开关", "key", "sys.account.registerUser", "value", "false",
                    "type", "built-in", "remark", "是否开放用户自助注册", "createdAt", "2024-01-05 09:00:00"),
            item("id", 3L, "name", "用户初始密码", "key", "sys.user.initPassword", "value", "123456",
                    "type", "built-in", "remark", "新用户默认密码", "createdAt", "2024-01-06 10:00:00")
    );

    private static final List<Map<String, Object>> NOTICES = List.of(
            item("id", 1L, "title", "系统新版本发布", "type", "公告", "status", "active", "author", "管理员",
                    "createdAt", "2026-08-20 09:00:00", "content", "本次版本统一了前后端 API 契约并补齐管理页面数据接口。"),
            item("id", 2L, "title", "晚间维护通知", "type", "通知", "status", "active", "author", "运维组",
                    "createdAt", "2026-08-28 14:00:00", "content", "计划于晚间进行数据库升级，期间服务可能短暂不可用。"),
            item("id", 3L, "title", "代码规范更新说明", "type", "通知", "status", "active", "author", "技术委员会",
                    "createdAt", "2026-08-18 11:00:00", "content", "提交前请运行 lint、typecheck 和 build。")
    );

    @Operation(summary = "仪表盘统计")
    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Object>> dashboardStats() {
        return ApiResponse.success(item(
                "todayVisits", 3256,
                "totalUsers", 12480,
                "activeUsers", 8934,
                "todayOrders", 156,
                "weeklyGrowth", 12.5,
                "monthlyRevenue", 284600,
                "conversionRate", 3.8,
                "systemUptime", 99.9,
                "weeklyTrend", item("days", List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
                        "visits", List.of(1200, 1380, 1520, 1290, 1680, 890, 720),
                        "users", List.of(320, 380, 420, 350, 460, 210, 180)),
                "roleDistribution", List.of(
                        item("name", "Admin", "value", 480), item("name", "Editor", "value", 1048),
                        item("name", "Viewer", "value", 2400), item("name", "Guest", "value", 735)),
                "topPages", List.of(
                        item("path", "/dashboard", "title", "仪表盘", "visits", 4520),
                        item("path", "/system/user", "title", "用户管理", "visits", 3180),
                        item("path", "/analytics", "title", "数据分析", "visits", 2860),
                        item("path", "/settings", "title", "系统设置", "visits", 1420)),
                "recentActivities", List.of(
                        item("id", 1L, "user", "admin", "action", "发布了文章《内容管理模块上线》", "time", LocalDateTime.now().minusMinutes(12).format(DATE_TIME_FORMAT)),
                        item("id", 2L, "user", "editor", "action", "更新了角色「运营」的权限", "time", LocalDateTime.now().minusMinutes(48).format(DATE_TIME_FORMAT)),
                        item("id", 3L, "user", "zhangsan", "action", "新增了菜单「内容管理」", "time", LocalDateTime.now().minusHours(2).format(DATE_TIME_FORMAT)),
                        item("id", 4L, "user", "lisi", "action", "导出了用户数据报表", "time", LocalDateTime.now().minusHours(5).format(DATE_TIME_FORMAT)),
                        item("id", 5L, "user", "admin", "action", "修改了系统参数配置", "time", LocalDateTime.now().minusHours(8).format(DATE_TIME_FORMAT)))));
    }

    @Operation(summary = "登录日志")
    @GetMapping("/log/login")
    public ApiResponse<PageResponse<Map<String, Object>>> loginLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] users = {"admin", "zhangsan", "lisi", "wangwu"};
        for (int i = 1; i <= 50; i++) {
            String username = users[i % users.length];
            rows.add(item("id", (long) i, "username", username, "ip", "192.168.1." + (10 + i),
                    "browser", i % 2 == 0 ? "Chrome 128" : "Safari 18", "os", i % 2 == 0 ? "Windows 11" : "macOS 15",
                    "status", i % 9 == 0 ? "fail" : "success", "message", i % 9 == 0 ? "密码错误" : "登录成功",
                    "createdAt", LocalDateTime.now().minusHours(i * 3L).format(DATE_TIME_FORMAT)));
        }
        return ApiResponse.success(page(filter(rows, keyword, "username"), page, pageSize));
    }

    @Operation(summary = "操作日志")
    @GetMapping("/log/operation")
    public ApiResponse<PageResponse<Map<String, Object>>> operationLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String module) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] modules = {"用户管理", "角色管理", "菜单管理", "系统设置", "日志管理"};
        String[] actions = {"新增", "编辑", "删除", "查询"};
        for (int i = 1; i <= 60; i++) {
            String moduleName = modules[i % modules.length];
            String action = actions[i % actions.length];
            rows.add(item("id", (long) i, "username", i % 2 == 0 ? "admin" : "editor", "module", moduleName,
                    "action", action, "description", action + "了" + moduleName + "的数据", "ip", "192.168.1." + (20 + i),
                    "method", Objects.equals(action, "查询") ? "GET" : Objects.equals(action, "删除") ? "DELETE" : "POST",
                    "status", i % 17 == 0 ? "fail" : "success", "duration", 20 + i * 7,
                    "createdAt", LocalDateTime.now().minusMinutes(i * 23L).format(DATE_TIME_FORMAT)));
        }
        List<Map<String, Object>> filtered = filter(rows, keyword, "username", "description").stream()
                .filter(row -> module.isBlank() || module.equals(row.get("module"))).toList();
        return ApiResponse.success(page(filtered, page, pageSize));
    }

    @Operation(summary = "前端错误日志")
    @GetMapping("/log/error")
    public ApiResponse<PageResponse<Map<String, Object>>> errorLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String status) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] types = {"TypeError", "ReferenceError", "NetworkError", "TimeoutError"};
        for (int i = 1; i <= 40; i++) {
            String rowStatus = i % 3 == 0 ? "pending" : "resolved";
            rows.add(item("id", (long) i, "type", types[i % types.length], "message", "Request failed at page " + i,
                    "page", i % 2 == 0 ? "/dashboard" : "/system/user", "stack", "Error: request failed\n    at setup (index.vue:42)",
                    "browser", "Chrome 128", "os", "macOS 15", "status", rowStatus,
                    "createdAt", LocalDateTime.now().minusHours(i * 4L).format(DATE_TIME_FORMAT)));
        }
        List<Map<String, Object>> filtered = rows.stream()
                .filter(row -> status.isBlank() || status.equals(row.get("status"))).toList();
        return ApiResponse.success(page(filtered, page, pageSize));
    }

    @Operation(summary = "系统参数列表")
    @GetMapping("/config/list")
    public ApiResponse<PageResponse<Map<String, Object>>> configs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(page(filter(CONFIGS, keyword, "name", "key"), page, pageSize));
    }

    @Operation(summary = "部门树")
    @GetMapping("/dept/list")
    public ApiResponse<List<Map<String, Object>>> departments() {
        return ApiResponse.success(List.of(item("id", 1L, "name", "Fast Vue3 集团", "leader", "管理员", "order", 0,
                "status", "active", "createdAt", "2024-01-01 08:00:00", "children", List.of(
                        item("id", 2L, "name", "技术部", "leader", "张三", "order", 1, "status", "active",
                                "createdAt", "2024-01-05 09:00:00"),
                        item("id", 3L, "name", "产品部", "leader", "李四", "order", 2, "status", "active",
                                "createdAt", "2024-01-08 09:30:00"),
                        item("id", 4L, "name", "运营部", "leader", "王五", "order", 3, "status", "inactive",
                                "createdAt", "2024-01-10 14:00:00")))));
    }

    @Operation(summary = "字典列表")
    @GetMapping("/dict/list")
    public ApiResponse<PageResponse<Map<String, Object>>> dictionaries(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        List<Map<String, Object>> rows = List.of(
                item("id", 1L, "name", "用户性别", "type", "sys_user_sex", "status", "active", "remark", "用户性别列表",
                        "createdAt", "2024-01-05 09:00:00", "data", List.of(item("label", "男", "value", "0"), item("label", "女", "value", "1"))),
                item("id", 2L, "name", "菜单状态", "type", "sys_show_hide", "status", "active", "remark", "菜单显示状态",
                        "createdAt", "2024-01-06 10:00:00", "data", List.of(item("label", "显示", "value", "0"), item("label", "隐藏", "value", "1"))),
                item("id", 3L, "name", "系统开关", "type", "sys_normal_disable", "status", "active", "remark", "系统开关列表",
                        "createdAt", "2024-01-07 11:00:00", "data", List.of(item("label", "正常", "value", "0"), item("label", "停用", "value", "1"))));
        return ApiResponse.success(page(filter(rows, keyword, "name", "type"), page, pageSize));
    }

    @Operation(summary = "通知公告列表")
    @GetMapping("/notice/list")
    public ApiResponse<PageResponse<Map<String, Object>>> notices(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String type) {
        List<Map<String, Object>> filtered = filter(NOTICES, keyword, "title").stream()
                .filter(row -> type.isBlank() || type.equals(row.get("type"))).toList();
        return ApiResponse.success(page(filtered, page, pageSize));
    }

    @Operation(summary = "在线用户")
    @GetMapping("/monitor/online")
    public ApiResponse<Map<String, Object>> onlineUsers() {
        List<Map<String, Object>> rows = List.of(
                item("id", 1L, "username", "admin", "realName", "管理员", "department", "技术部", "ip", "192.168.1.10",
                        "browser", "Chrome 128", "os", "macOS 15", "loginAt", "2026-08-30 08:55:12"),
                item("id", 2L, "username", "zhangsan", "realName", "张三", "department", "产品部", "ip", "192.168.1.23",
                        "browser", "Edge 127", "os", "Windows 11", "loginAt", "2026-08-30 09:02:40"));
        return ApiResponse.success(item("items", rows, "total", rows.size()));
    }

    @Operation(summary = "服务运行信息")
    @GetMapping("/monitor/server")
    public ApiResponse<Map<String, Object>> serverInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        double usage = totalMemory == 0 ? 0 : usedMemory * 100.0 / totalMemory;
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return ApiResponse.success(item(
                "cpu", item("usage", 0, "cores", runtime.availableProcessors(), "model", System.getProperty("os.arch")),
                "memory", item("total", formatBytes(totalMemory), "used", formatBytes(usedMemory), "usage", Math.round(usage * 10) / 10.0),
                "disk", item("total", "N/A", "used", "N/A", "usage", 0),
                "runtime", item("node", System.getProperty("java.version"), "os", System.getProperty("os.name"),
                        "uptime", uptimeSeconds + " 秒", "port", 8080),
                "trend", List.of()));
    }

    private static List<Map<String, Object>> filter(List<Map<String, Object>> source, String keyword, String... keys) {
        if (keyword == null || keyword.isBlank()) {
            return source;
        }
        return source.stream().filter(row -> {
            for (String key : keys) {
                if (String.valueOf(row.getOrDefault(key, "")).contains(keyword)) {
                    return true;
                }
            }
            return false;
        }).toList();
    }

    private static <T> PageResponse<T> page(List<T> source, long page, long pageSize) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        int from = (int) Math.min(source.size(), (safePage - 1) * safePageSize);
        int to = (int) Math.min(source.size(), from + safePageSize);
        return PageResponse.of(source.subList(from, to), safePage, safePageSize, source.size());
    }

    private static Map<String, Object> item(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("entries must contain key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return result;
    }

    private static String formatBytes(long bytes) {
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
