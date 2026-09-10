package com.fastvue.module.portal.api;

import com.fastvue.common.response.ApiResponse;
import com.fastvue.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 门户站公开接口。
 *
 * <p>包含博客、常见问答、套餐、功能特性、关于、产品、文档、首页与联系表单等，
 * 全部无需登录即可访问（已在 {@code SecurityConfig} 中放行）。</p>
 */
@Tag(name = "门户站公开数据")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PortalController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<PortalModels.BlogPost> BLOG_POSTS = List.of(
            new PortalModels.BlogPost("张明", "实战教程",
                    List.of("很多企业后台的痛点不是功能，而是从 0 到 1 的启动成本。",
                            "本文以订单管理后台为例，带你走完一条可投产的最短路径。"),
                    "", "2026-08-12", "从脚手架到权限、内容管理，一天搭好企业后台。",
                    1L, List.of("教程", "后台", "CLI"),
                    "如何用 Fast Vue3 在一天内搭好企业后台"),
            new PortalModels.BlogPost("李雪", "架构解析",
                    List.of("功能对齐是 Fast Vue3 的核心约束：无论使用哪套 UI，后台都拥有相同的信息架构。",
                            "业务逻辑沉淀在共享包里，UI 层只负责渲染同一份数据结构。"),
                    "", "2026-08-06", "在不牺牲各框架原生体验的前提下，让页面能力完全对齐。",
                    2L, List.of("架构", "UI", "Design System"),
                    "7 套 UI 框架，同一套业务能力的秘密"),
            new PortalModels.BlogPost("王浩", "工程实践",
                    List.of("在大型后台里，路由表往往是最容易腐化的文件。",
                            "动态参数用方括号文件名表达，配合侧边栏目录分组，路由与导航始终一致。"),
                    "", "2026-07-29", "告别手写路由表，目录即路由。",
                    3L, List.of("路由", "工程化"),
                    "文件路由让新增页面变成建文件"),
            new PortalModels.BlogPost("陈思", "设计",
                    List.of("暗色模式翻车，多半是因为把颜色写死在了组件里。",
                            "切换主题时只改语义变量，组件便可全部响应。"),
                    "", "2026-07-21", "把颜色抽象为语义 Token，让组件在明暗主题间无缝切换。",
                    4L, List.of("主题", "CSS", "Design Token"),
                    "暗色模式不只是换肤：设计 Token 的正确打开方式"),
            new PortalModels.BlogPost("刘洋", "工程实践",
                    List.of("工程同时存在多套框架变体时，全量重复构建不可接受。",
                            "Turborepo 通过任务图谱只重建受影响的包，其余结果直接命中缓存。"),
                    "", "2026-07-14", "任务编排、远程缓存与 affected 检测，让构建快到飞起。",
                    5L, List.of("Turborepo", "构建", "Monorepo"),
                    "Turborepo 远程缓存：让 CI 快到飞起"),
            new PortalModels.BlogPost("赵敏", "团队观点",
                    List.of("占位文本会掩盖中文与英文在行高、断行和标点上的真实差异。",
                            "模板坚持使用真实中文业务内容。"),
                    "", "2026-07-08", "占位文本会掩盖真实的排版与信息密度问题。",
                    6L, List.of("内容", "团队"),
                    "真实中文内容：为什么我们不写 Lorem Ipsum"),
            new PortalModels.BlogPost("周凯", "团队故事",
                    List.of("新人从小改动开始，逐步过渡到列表筛选，再到独立负责完整模块。",
                            "持续的小步反馈与可靠脚手架同样重要。"),
                    "", "2026-06-30", "小步反馈加可信任的脚手架，让新人逐步交付完整模块。",
                    7L, List.of("成长", "招聘"),
                    "从 PR 到独立负责模块：实习生培养路径"),
            new PortalModels.BlogPost("李雪", "产品动态",
                    List.of("组件市场覆盖高级搜索、可拖拽看板和角色权限矩阵等高频场景。",
                            "一行命令安装，并自动适配框架设计语言。"),
                    "", "2026-06-22", "生产验证的业务区块，安装后自动适配所选 UI 框架。",
                    8L, List.of("组件市场", "效率"),
                    "组件市场来了：把业务区块装进一行命令"),
            new PortalModels.BlogPost("王浩", "公告",
                    List.of("核心共享包采用 Apache-2.0，提供更明确的专利授权与商业保障。",
                            "个人与商业项目均可继续使用。"),
                    "", "2026-06-12", "更明确的专利授权与商业保障，使用方式保持不变。",
                    9L, List.of("开源", "协议"),
                    "开源协议变更意味着什么"));

    @Operation(summary = "门户博客列表")
    @GetMapping("/blog")
    public ApiResponse<PortalModels.BlogListResult> blog(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long pageSize,
            @RequestParam(defaultValue = "全部") String category) {
        List<PortalModels.BlogPost> filtered = BLOG_POSTS.stream()
                .filter(post -> category == null || category.isBlank() || category.equals("全部")
                        || category.equals(post.category()))
                .toList();
        PageResponse<PortalModels.BlogPost> pageData = page(filtered, page, pageSize);

        Set<String> categories = new LinkedHashSet<>();
        categories.add("全部");
        BLOG_POSTS.stream().map(PortalModels.BlogPost::category).forEach(categories::add);

        return ApiResponse.success(new PortalModels.BlogListResult(
                pageData.items(), pageData.page(), pageData.pageSize(), pageData.total(),
                new ArrayList<>(categories)));
    }

    @Operation(summary = "门户博客详情")
    @GetMapping("/blog/{id}")
    public ApiResponse<PortalModels.BlogPost> blogDetail(@PathVariable long id) {
        return BLOG_POSTS.stream()
                .filter(post -> post.id() == id)
                .findFirst()
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(com.fastvue.common.exception.ErrorCode.NOT_FOUND, "文章不存在"));
    }

    @Operation(summary = "提交联系表单")
    @PostMapping("/contact")
    public ApiResponse<PortalModels.ContactResult> contact(@RequestBody PortalModels.ContactParams request) {
        return ApiResponse.success(new PortalModels.ContactResult(
                true, request.email(), LocalDateTime.now().format(DATE_TIME_FORMAT)));
    }

    @Operation(summary = "常见问答列表")
    @GetMapping("/faq")
    public ApiResponse<List<PortalModels.FaqItem>> faq() {
        return ApiResponse.success(List.of(
                new PortalModels.FaqItem(1L, "Fast Vue3 适合什么类型的项目？",
                        "适合需要多套 UI 但共用一套业务能力的企业后台、中台与门户站。", "产品"),
                new PortalModels.FaqItem(2L, "是否必须购买商业授权？",
                        "核心共享包采用 Apache-2.0，个人与商业项目均可免费使用。", "计费"),
                new PortalModels.FaqItem(3L, "前后端如何对接？",
                        "后端返回统一的 { code, message, data } 信封，前端通过 VITE_APP_API_BASEURL 切换数据源。", "技术"),
                new PortalModels.FaqItem(4L, "支持哪些 UI 框架？",
                        "当前覆盖 Element Plus、Ant Design Vue 等主流方案，更多框架持续接入。", "产品"),
                new PortalModels.FaqItem(5L, "如何部署到生产环境？",
                        "提供 Docker Compose 与容器化构建，配合环境变量即可完成部署。", "部署"),
                new PortalModels.FaqItem(6L, "是否支持权限与菜单动态配置？",
                        "支持基于 RBAC 的权限字符串与菜单树，管理员可在后台动态维护。", "技术"),
                new PortalModels.FaqItem(7L, "能否私有化部署？",
                        "可以，整套服务可完全私有化部署，数据不出内网。", "部署"),
                new PortalModels.FaqItem(8L, "如何获取技术支持？",
                        "可通过官方社区、文档站与工单系统获取支持。", "计费")));
    }

    @Operation(summary = "套餐价格列表")
    @GetMapping("/pricing")
    public ApiResponse<List<PortalModels.PricingPlan>> pricing() {
        return ApiResponse.success(List.of(
                new PortalModels.PricingPlan(1L, "免费版", "适合个人学习与小型项目验证",
                        "¥0", "永久", List.of("单套 UI 框架", "基础组件", "社区支持"), false),
                new PortalModels.PricingPlan(2L, "团队版", "适合中小团队协同开发",
                        "¥299", "年", List.of("全部 UI 框架", "组件市场", "权限与菜单模块", "工单支持"), true),
                new PortalModels.PricingPlan(3L, "企业版", "适合中大型企业私有化部署",
                        "¥999", "年", List.of("团队版全部能力", "私有化部署", "SSO 对接", "专属技术支持"), false),
                new PortalModels.PricingPlan(4L, "旗舰版", "适合有定制与合规需求的组织",
                        "定制", "面议", List.of("企业版全部能力", "源码级定制", "合规审计", "专人服务"), false)));
    }

    @Operation(summary = "功能特性列表")
    @GetMapping("/features")
    public ApiResponse<List<PortalModels.FeatureItem>> features() {
        return ApiResponse.success(List.of(
                new PortalModels.FeatureItem(1L, "多 UI 框架对齐", "同一份业务能力，自动适配各框架原生组件。", "AppstoreOutlined"),
                new PortalModels.FeatureItem(2L, "RBAC 权限", "权限字符串 + 菜单树，细粒度控制页面与按钮。", "SafetyOutlined"),
                new PortalModels.FeatureItem(3L, "文件路由", "目录即路由，新增页面变成新建文件。", "FolderOpenOutlined"),
                new PortalModels.FeatureItem(4L, "暗色模式", "基于设计 Token，明暗主题无缝切换。", "BulbOutlined"),
                new PortalModels.FeatureItem(5L, "组件市场", "一行命令安装生产验证的业务区块。", "ThunderboltOutlined"),
                new PortalModels.FeatureItem(6L, "可观测性", "内置日志、监控与数据看板。", "DashboardOutlined"),
                new PortalModels.FeatureItem(7L, "容器化部署", "Docker Compose 一键拉起前后端与数据库。", "CloudOutlined"),
                new PortalModels.FeatureItem(8L, "类型安全", "前后端共享接口契约类型，对接零歧义。", "CheckCircleOutlined")));
    }

    @Operation(summary = "关于我们")
    @GetMapping("/about")
    public ApiResponse<PortalModels.AboutInfo> about() {
        return ApiResponse.success(new PortalModels.AboutInfo(
                "Fast Vue3 致力于让企业后台从 0 到 1 的启动成本降到最低，"
                        + "在保持各 UI 框架原生体验的同时，让业务能力与信息架构完全对齐。",
                List.of(
                        new PortalModels.Stat("服务企业", "2000+"),
                        new PortalModels.Stat("开源 Star", "18k"),
                        new PortalModels.Stat("贡献者", "320+"),
                        new PortalModels.Stat("组件区块", "150+")),
                List.of(
                        new PortalModels.Team("张明", "创始人 / CEO", ""),
                        new PortalModels.Team("李雪", "架构负责人", ""),
                        new PortalModels.Team("王浩", "工程负责人", ""),
                        new PortalModels.Team("陈思", "设计负责人", "")),
                List.of(
                        new PortalModels.Milestone("2023-03", "项目启动", "确定模块化单体与服务端参考实现方向。"),
                        new PortalModels.Milestone("2024-06", "开源发布", "首个公开版本发布，覆盖 5 套 UI 框架。"),
                        new PortalModels.Milestone("2025-09", "组件市场上线", "业务区块可一行命令安装并自动适配框架。"),
                        new PortalModels.Milestone("2026-08", "官方后端补齐", "Spring Boot 3 参考后端对齐全部前端契约。"))));
    }

    @Operation(summary = "产品信息")
    @GetMapping("/product")
    public ApiResponse<PortalModels.ProductInfo> product() {
        return ApiResponse.success(new PortalModels.ProductInfo(
                "Fast Vue3",
                "多 UI 框架共享同一套业务能力的企业后台方案",
                List.of(
                        new PortalModels.Highlight("能力对齐", "无论使用哪套 UI，后台都拥有相同的信息架构与功能。"),
                        new PortalModels.Highlight("启动极快", "脚手架 + 文件路由，让你一天跑通首条业务链路。"),
                        new PortalModels.Highlight("易于维护", "业务逻辑沉淀在共享包，UI 层只负责渲染。"))));
    }

    @Operation(summary = "文档目录")
    @GetMapping("/docs")
    public ApiResponse<List<PortalModels.DocSection>> docs() {
        return ApiResponse.success(List.of(
                new PortalModels.DocSection(1L, "快速开始",
                        "从零跑通第一个后台页面。",
                        List.of(
                                new PortalModels.DocItem("/docs/intro", "项目介绍"),
                                new PortalModels.DocItem("/docs/install", "安装与初始化"),
                                new PortalModels.DocItem("/docs/first-page", "创建第一个页面"))),
                new PortalModels.DocSection(2L, "核心概念",
                        "理解框架的设计约束。",
                        List.of(
                                new PortalModels.DocItem("/docs/architecture", "架构总览"),
                                new PortalModels.DocItem("/docs/file-routing", "文件路由"),
                                new PortalModels.DocItem("/docs/design-token", "设计 Token"))),
                new PortalModels.DocSection(3L, "模块指南",
                        "按业务模块开发。",
                        List.of(
                                new PortalModels.DocItem("/docs/auth", "认证与授权"),
                                new PortalModels.DocItem("/docs/rbac", "RBAC 权限"),
                                new PortalModels.DocItem("/docs/content", "内容管理"))),
                new PortalModels.DocSection(4L, "部署运维",
                        "生产环境相关。",
                        List.of(
                                new PortalModels.DocItem("/docs/docker", "容器化部署"),
                                new PortalModels.DocItem("/docs/config", "环境变量配置"),
                                new PortalModels.DocItem("/docs/observability", "监控与日志")))));
    }

    @Operation(summary = "首页信息")
    @GetMapping("/home")
    public ApiResponse<PortalModels.HomeInfo> home() {
        return ApiResponse.success(new PortalModels.HomeInfo(
                List.of(
                        new PortalModels.Highlight("能力对齐", "多套 UI 框架共享同一套业务能力。"),
                        new PortalModels.Highlight("极速启动", "一天跑通可投产的后台。"),
                        new PortalModels.Highlight("生态丰富", "组件市场提供 150+ 业务区块。")),
                List.of(
                        new PortalModels.Stat("服务企业", "2000+"),
                        new PortalModels.Stat("开源 Star", "18k"),
                        new PortalModels.Stat("组件区块", "150+"),
                        new PortalModels.Stat("贡献者", "320+")),
                List.of(
                        new PortalModels.Testimonial("接入后我们用一周就交付了三套不同 UI 的后台。", "刘洋", "某 SaaS 公司技术负责人"),
                        new PortalModels.Testimonial("权限与菜单模块省去了大量重复工作。", "陈思", "某互联网公司前端负责人"),
                        new PortalModels.Testimonial("文档清晰，新人也能快速上手。", "周凯", "某创业团队负责人"))));
    }

    private static <T> PageResponse<T> page(List<T> source, long page, long pageSize) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        int from = (int) Math.min(source.size(), (safePage - 1) * safePageSize);
        int to = (int) Math.min(source.size(), from + safePageSize);
        return PageResponse.of(source.subList(from, to), safePage, safePageSize, source.size());
    }
}
