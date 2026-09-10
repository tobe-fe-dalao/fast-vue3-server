package com.fastvue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces complete, language-specific OpenAPI documents without coupling business controllers to i18n.
 */
final class OpenApiDocumentationCustomizer implements OpenApiCustomizer {

    enum Language {
        ZH_CN, JA_JP, EN_US
    }

    private record Text(String zh, String ja, String en) {
        String value(Language language) {
            return switch (language) {
                case ZH_CN -> zh;
                case JA_JP -> ja;
                case EN_US -> en;
            };
        }
    }

    private record TagDoc(Text name, Text description) {
    }

    private static final Map<String, TagDoc> TAGS = tags();
    private static final Map<String, Text> OPERATIONS = operations();
    private static final Map<String, Text> FIELDS = fields();

    private final Language language;

    OpenApiDocumentationCustomizer(Language language) {
        this.language = language;
    }

    @Override
    public void customise(OpenAPI openApi) {
        configureInfo(openApi);
        configureSecurityScheme(openApi);

        if (openApi.getPaths() == null) {
            return;
        }

        Set<String> usedTags = new LinkedHashSet<>();
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap()
                .forEach((method, operation) -> localizeOperation(path, method, operation, usedTags)));
        openApi.setTags(buildTags(usedTags));
        localizeSchemas(openApi);
    }

    private void configureInfo(OpenAPI openApi) {
        openApi.setInfo(new Info()
                .title(text("fast-vue3-server API 文档", "fast-vue3-server API ドキュメント", "fast-vue3-server API Documentation"))
                .description(text(
                        "Fast Vue3 的 Java 21 / Spring Boot 参考后端。接口统一使用 `/api/v1` 前缀和 `{ code, message, data }` 响应结构。受保护接口需要 Bearer JWT。",
                        "Fast Vue3 の Java 21 / Spring Boot リファレンスバックエンドです。すべての API は `/api/v1` プレフィックスと `{ code, message, data }` レスポンス形式を使用します。保護された API には Bearer JWT が必要です。",
                        "Java 21 / Spring Boot reference backend for Fast Vue3. All APIs use the `/api/v1` prefix and the `{ code, message, data }` response envelope. Protected operations require a Bearer JWT."))
                .version("0.1.0"));
    }

    private void configureSecurityScheme(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSecuritySchemes() == null) {
            return;
        }
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);
        if (scheme != null) {
            scheme.setDescription(text(
                    "填写登录或刷新令牌接口返回的 Access Token，无需输入 `Bearer ` 前缀。",
                    "ログインまたはトークン更新 API が返す Access Token を入力してください。`Bearer ` プレフィックスは不要です。",
                    "Enter the Access Token returned by the login or refresh operation. Do not include the `Bearer ` prefix."));
        }
    }

    private void localizeOperation(
            String path,
            PathItem.HttpMethod method,
            Operation operation,
            Set<String> usedTags) {
        String tagKey = tagFor(path);
        TagDoc tag = TAGS.get(tagKey);
        operation.setTags(List.of(tag.name().value(language)));
        usedTags.add(tagKey);

        Text summary = OPERATIONS.get(operationKey(method, path));
        String localizedSummary = summary == null
                ? fallbackSummary(method, path)
                : summary.value(language);
        operation.setSummary(localizedSummary);
        operation.setDescription(operationDescription(localizedSummary, isPublic(path, method)));

        boolean publicEndpoint = isPublic(path, method);
        operation.setSecurity(publicEndpoint
                ? Collections.emptyList()
                : List.of(new SecurityRequirement().addList(OpenApiConfig.SECURITY_SCHEME_NAME)));

        localizeParameters(operation);
        if (operation.getRequestBody() != null && operation.getRequestBody().getDescription() == null) {
            operation.getRequestBody().setDescription(text("请求数据", "リクエストデータ", "Request payload"));
        }
        localizeResponses(path, method, operation, publicEndpoint);
    }

    private void localizeParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        for (Parameter parameter : operation.getParameters()) {
            Text field = FIELDS.get(parameter.getName());
            if (field != null) {
                parameter.setDescription(field.value(language));
            }
            if (parameter.getExample() == null) {
                Object example = example(parameter.getName());
                if (example != null) {
                    parameter.setExample(example);
                }
            }
        }
    }

    private void localizeResponses(
            String path,
            PathItem.HttpMethod method,
            Operation operation,
            boolean publicEndpoint) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.forEach((code, response) -> {
            if (code.startsWith("2")) {
                response.setDescription(text("请求成功", "リクエスト成功", "Request succeeded"));
            }
        });
        addResponse(responses, "400", text("请求参数错误", "リクエストパラメータが不正です", "Invalid request parameters"));
        if (!publicEndpoint) {
            addResponse(responses, "401", text("未登录或 Access Token 无效", "未認証、または Access Token が無効です", "Missing or invalid Access Token"));
            addResponse(responses, "403", text("已登录，但没有操作权限", "認証済みですが操作権限がありません", "Authenticated but not authorized for this operation"));
        }
        if (path.contains("{") || method != PathItem.HttpMethod.GET) {
            addResponse(responses, "404", text("目标资源不存在", "対象リソースが見つかりません", "Target resource was not found"));
        }
        if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT
                || method == PathItem.HttpMethod.PATCH || method == PathItem.HttpMethod.DELETE) {
            addResponse(responses, "409", text("资源状态冲突或重复提交", "リソース状態の競合、または重複送信です", "Resource state conflict or duplicate submission"));
        }
        addResponse(responses, "429", text("请求过于频繁", "リクエストが多すぎます", "Too many requests"));
        addResponse(responses, "500", text("服务器内部错误", "サーバー内部エラー", "Internal server error"));
    }

    private void addResponse(ApiResponses responses, String code, String description) {
        responses.putIfAbsent(code, new ApiResponse().description(description));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void localizeSchemas(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (schema.getTitle() == null) {
                schema.setTitle(name);
            }
            if (schema.getDescription() == null) {
                schema.setDescription(text("接口数据模型", "API データモデル", "API data model"));
            }
            Map<String, Schema> properties = schema.getProperties();
            if (properties == null) {
                return;
            }
            properties.forEach((propertyName, propertySchema) -> {
                Text field = FIELDS.get(propertyName);
                if (field != null) {
                    propertySchema.setDescription(field.value(language));
                }
                if (propertySchema.getExample() == null) {
                    Object example = example(propertyName);
                    if (example != null) {
                        propertySchema.setExample(example);
                    }
                }
            });
        });
    }

    private List<Tag> buildTags(Set<String> usedTags) {
        List<Tag> result = new ArrayList<>();
        TAGS.forEach((key, value) -> {
            if (usedTags.contains(key)) {
                result.add(new Tag()
                        .name(value.name().value(language))
                        .description(value.description().value(language)));
            }
        });
        return result;
    }

    private String operationDescription(String summary, boolean publicEndpoint) {
        return switch (language) {
            case ZH_CN -> summary + "。" + (publicEndpoint
                    ? "该接口无需登录。"
                    : "需要在 `Authorization` 请求头中携带 Bearer Access Token。")
                    + "成功响应使用统一的 `{ code, message, data }` 结构。";
            case JA_JP -> summary + "。" + (publicEndpoint
                    ? "この API はログイン不要です。"
                    : "`Authorization` ヘッダーに Bearer Access Token が必要です。")
                    + "成功時は共通の `{ code, message, data }` 形式で返します。";
            case EN_US -> summary + ". " + (publicEndpoint
                    ? "No authentication is required. "
                    : "Send a Bearer Access Token in the `Authorization` header. ")
                    + "Successful responses use the common `{ code, message, data }` envelope.";
        };
    }

    private String fallbackSummary(PathItem.HttpMethod method, String path) {
        return switch (language) {
            case ZH_CN -> method + " " + path;
            case JA_JP -> method + " " + path;
            case EN_US -> method + " " + path;
        };
    }

    private boolean isPublic(String path, PathItem.HttpMethod method) {
        if (path.startsWith("/api/v1/public/")) {
            return method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.POST;
        }
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh");
    }

    private String tagFor(String path) {
        if (path.startsWith("/api/v1/auth")) return "auth";
        if (path.startsWith("/api/v1/users")) return "users";
        if (path.startsWith("/api/v1/roles")) return "roles";
        if (path.startsWith("/api/v1/permissions")) return "permissions";
        if (path.startsWith("/api/v1/menus")) return "menus";
        if (path.startsWith("/api/v1/tenants")) return "tenants";
        if (path.startsWith("/api/v1/organizations")) return "organizations";
        if (path.startsWith("/api/v1/departments")) return "departments";
        if (path.contains("/tasks") || path.startsWith("/api/v1/tasks")) return "tasks";
        if (path.startsWith("/api/v1/projects")) return "projects";
        if (path.startsWith("/api/v1/approvals")) return "approvals";
        if (path.startsWith("/api/v1/notifications")) return "notifications";
        if (path.startsWith("/api/v1/content/articles")) return "articles";
        if (path.startsWith("/api/v1/content/categories")) return "categories";
        if (path.startsWith("/api/v1/analytics")) return "analytics";
        if (path.startsWith("/api/v1/data")) return "data";
        if (path.startsWith("/api/v1/audit")) return "audit";
        if (path.startsWith("/api/v1/files")) return "files";
        if (path.contains("/comments") || path.startsWith("/api/v1/blog") || path.startsWith("/api/v1/payments")) return "site";
        if (path.startsWith("/api/v1/public")) return "portal";
        return "demo";
    }

    private static String operationKey(PathItem.HttpMethod method, String path) {
        return method.name() + " " + path;
    }

    private String text(String zh, String ja, String en) {
        return new Text(zh, ja, en).value(language);
    }

    private static Text t(String zh, String ja, String en) {
        return new Text(zh, ja, en);
    }

    private static Map<String, TagDoc> tags() {
        Map<String, TagDoc> values = new LinkedHashMap<>();
        tag(values, "auth", "认证", "認証", "Authentication", "登录、注册、令牌刷新、登出与当前用户信息。", "ログイン、登録、トークン更新、ログアウト、現在のユーザー情報。", "Login, registration, token refresh, logout, and current-user operations.");
        tag(values, "users", "用户管理", "ユーザー管理", "Users", "管理用户资料、状态及角色绑定。", "ユーザー情報、状態、ロール割り当てを管理します。", "Manage user profiles, status, and role assignments.");
        tag(values, "roles", "角色管理", "ロール管理", "Roles", "管理角色以及角色关联的权限和菜单。", "ロールと、関連する権限・メニューを管理します。", "Manage roles and their permission and menu assignments.");
        tag(values, "permissions", "权限管理", "権限管理", "Permissions", "管理细粒度 RBAC 权限。", "細粒度の RBAC 権限を管理します。", "Manage fine-grained RBAC permissions.");
        tag(values, "menus", "菜单管理", "メニュー管理", "Menus", "管理前端路由菜单树和按钮权限。", "フロントエンドのルートメニュー階層とボタン権限を管理します。", "Manage the frontend route tree and button permissions.");
        tag(values, "tenants", "租户管理", "テナント管理", "Tenants", "创建和维护多租户账户。", "マルチテナントアカウントを作成・管理します。", "Create and maintain multi-tenant accounts.");
        tag(values, "organizations", "组织管理", "組織管理", "Organizations", "查询和更新当前租户的组织信息。", "現在のテナントの組織情報を取得・更新します。", "Read and update organization information for the current tenant.");
        tag(values, "departments", "部门管理", "部門管理", "Departments", "管理部门树及部门成员。", "部門ツリーと部門メンバーを管理します。", "Manage the department tree and department members.");
        tag(values, "projects", "项目管理", "プロジェクト管理", "Projects", "管理项目、成员、归档状态和项目活动。", "プロジェクト、メンバー、アーカイブ状態、アクティビティを管理します。", "Manage projects, members, archive state, and project activity.");
        tag(values, "tasks", "任务管理", "タスク管理", "Tasks", "管理项目任务、评论、状态和变更记录。", "プロジェクトタスク、コメント、状態、変更履歴を管理します。", "Manage project tasks, comments, state, and activity history.");
        tag(values, "approvals", "审批管理", "承認管理", "Approvals", "创建、提交、同意、拒绝或取消审批。", "承認申請の作成、提出、承認、却下、取消を行います。", "Create, submit, approve, reject, or cancel approval requests.");
        tag(values, "notifications", "通知中心", "通知センター", "Notifications", "查询通知并维护已读状态。", "通知を取得し、既読状態を管理します。", "Read notifications and maintain their read state.");
        tag(values, "articles", "内容管理－文章", "コンテンツ管理－記事", "Content - Articles", "管理文章内容、分类、标签和发布状态。", "記事内容、カテゴリ、タグ、公開状態を管理します。", "Manage article content, categories, tags, and publication state.");
        tag(values, "categories", "内容管理－分类", "コンテンツ管理－カテゴリ", "Content - Categories", "管理文章分类。", "記事カテゴリを管理します。", "Manage article categories.");
        tag(values, "analytics", "数据分析", "データ分析", "Analytics", "提供指定时间范围内的业务分析指标。", "指定期間のビジネス分析指標を提供します。", "Provide business analytics for a selected time range.");
        tag(values, "data", "数据中心", "データセンター", "Data Center", "提供数据中心概览和关键指标。", "データセンターの概要と主要指標を提供します。", "Provide data-center overview and key metrics.");
        tag(values, "audit", "审计日志", "監査ログ", "Audit Log", "查询不包含敏感请求体的操作审计记录。", "機密リクエスト本文を含まない操作監査ログを取得します。", "Read operation audit records that exclude sensitive request bodies.");
        tag(values, "files", "文件存储", "ファイルストレージ", "Files", "上传、读取和删除租户隔离的文件。", "テナント分離されたファイルをアップロード、取得、削除します。", "Upload, read, and delete tenant-isolated files.");
        tag(values, "site", "站点交互", "サイト操作", "Site Interactions", "处理博客评论和演示支付订单。", "ブログコメントとデモ決済注文を処理します。", "Handle blog comments and demonstration checkout orders.");
        tag(values, "portal", "门户公开接口", "公開ポータル API", "Public Portal", "无需登录的门户站内容接口。", "ログイン不要のポータルコンテンツ API です。", "Public portal content operations that do not require authentication.");
        tag(values, "demo", "演示与系统数据", "デモ・システムデータ", "Demo and System Data", "为管理端演示页面提供仪表盘、日志、字典和监控数据。", "管理画面のデモ用にダッシュボード、ログ、辞書、監視データを提供します。", "Provide dashboard, log, dictionary, and monitoring data for the admin demo.");
        return Collections.unmodifiableMap(values);
    }

    private static void tag(Map<String, TagDoc> values, String key,
                            String zhName, String jaName, String enName,
                            String zhDescription, String jaDescription, String enDescription) {
        values.put(key, new TagDoc(t(zhName, jaName, enName), t(zhDescription, jaDescription, enDescription)));
    }

    private static Map<String, Text> operations() {
        Map<String, Text> values = new LinkedHashMap<>();
        op(values, "POST", "/api/v1/auth/login", "用户登录", "ログイン", "Log in");
        op(values, "POST", "/api/v1/auth/register", "注册用户", "ユーザー登録", "Register a user");
        op(values, "POST", "/api/v1/auth/refresh", "刷新访问令牌", "アクセストークンを更新", "Refresh access token");
        op(values, "POST", "/api/v1/auth/logout", "用户登出", "ログアウト", "Log out");
        op(values, "GET", "/api/v1/auth/me", "查询当前用户", "現在のユーザーを取得", "Get current user");

        crud(values, "/api/v1/users", "用户", "ユーザー", "user", true);
        crud(values, "/api/v1/roles", "角色", "ロール", "role", true);
        listCreateUpdateDelete(values, "/api/v1/permissions", "权限", "権限", "permission");
        op(values, "GET", "/api/v1/menus", "查询当前用户菜单树", "現在のユーザーメニューツリーを取得", "Get current-user menu tree");
        op(values, "GET", "/api/v1/menus/tree", "查询全部菜单树", "すべてのメニューツリーを取得", "Get the complete menu tree");
        op(values, "POST", "/api/v1/menus", "创建菜单", "メニューを作成", "Create a menu");
        crudItem(values, "/api/v1/menus", "菜单", "メニュー", "menu");

        op(values, "GET", "/api/v1/tenants", "查询租户列表", "テナント一覧を取得", "List tenants");
        op(values, "POST", "/api/v1/tenants", "创建租户", "テナントを作成", "Create a tenant");
        op(values, "PUT", "/api/v1/tenants/{id}", "更新租户", "テナントを更新", "Update a tenant");
        op(values, "GET", "/api/v1/organizations/current", "查询当前组织", "現在の組織を取得", "Get current organization");
        op(values, "PUT", "/api/v1/organizations/{id}", "更新组织", "組織を更新", "Update an organization");
        op(values, "GET", "/api/v1/departments", "查询部门树", "部門ツリーを取得", "Get department tree");
        op(values, "GET", "/api/v1/departments/{id}/members", "查询部门成员", "部門メンバーを取得", "List department members");
        op(values, "POST", "/api/v1/departments", "创建部门", "部門を作成", "Create a department");
        op(values, "PUT", "/api/v1/departments/{id}", "更新部门", "部門を更新", "Update a department");
        op(values, "PUT", "/api/v1/departments/{id}/members/{userId}", "分配部门成员", "部門メンバーを割り当て", "Assign a department member");
        op(values, "DELETE", "/api/v1/departments/{id}/members/{userId}", "移除部门成员", "部門メンバーを削除", "Remove a department member");
        op(values, "DELETE", "/api/v1/departments/{id}", "删除部门", "部門を削除", "Delete a department");

        op(values, "GET", "/api/v1/projects", "查询项目列表", "プロジェクト一覧を取得", "List projects");
        op(values, "GET", "/api/v1/projects/{id}", "查询项目详情", "プロジェクト詳細を取得", "Get project details");
        op(values, "GET", "/api/v1/projects/{id}/activities", "查询项目活动", "プロジェクト活動を取得", "List project activity");
        op(values, "POST", "/api/v1/projects", "创建项目", "プロジェクトを作成", "Create a project");
        op(values, "PUT", "/api/v1/projects/{id}", "更新项目", "プロジェクトを更新", "Update a project");
        op(values, "PUT", "/api/v1/projects/{id}/archive", "归档项目", "プロジェクトをアーカイブ", "Archive a project");
        op(values, "PUT", "/api/v1/projects/{id}/members/{userId}", "添加项目成员", "プロジェクトメンバーを追加", "Add a project member");
        op(values, "DELETE", "/api/v1/projects/{id}/members/{userId}", "移除项目成员", "プロジェクトメンバーを削除", "Remove a project member");

        op(values, "GET", "/api/v1/projects/{projectId}/tasks", "查询项目任务", "プロジェクトタスクを取得", "List project tasks");
        op(values, "POST", "/api/v1/projects/{projectId}/tasks", "创建项目任务", "プロジェクトタスクを作成", "Create a project task");
        op(values, "GET", "/api/v1/tasks/{id}", "查询任务详情", "タスク詳細を取得", "Get task details");
        op(values, "PUT", "/api/v1/tasks/{id}", "更新任务", "タスクを更新", "Update a task");
        op(values, "POST", "/api/v1/tasks/{id}/comments", "添加任务评论", "タスクコメントを追加", "Add a task comment");
        op(values, "GET", "/api/v1/tasks/{id}/comments", "查询任务评论", "タスクコメントを取得", "List task comments");
        op(values, "GET", "/api/v1/tasks/{id}/activities", "查询任务活动", "タスク活動を取得", "List task activity");

        op(values, "GET", "/api/v1/approvals", "查询审批列表", "承認申請一覧を取得", "List approval requests");
        op(values, "POST", "/api/v1/approvals", "创建审批", "承認申請を作成", "Create an approval request");
        op(values, "POST", "/api/v1/approvals/{id}/submit", "提交审批", "承認申請を提出", "Submit an approval request");
        op(values, "POST", "/api/v1/approvals/{id}/approve", "同意审批", "承認申請を承認", "Approve a request");
        op(values, "POST", "/api/v1/approvals/{id}/reject", "拒绝审批", "承認申請を却下", "Reject a request");
        op(values, "POST", "/api/v1/approvals/{id}/cancel", "取消审批", "承認申請を取消", "Cancel an approval request");

        op(values, "GET", "/api/v1/notifications", "查询通知列表", "通知一覧を取得", "List notifications");
        op(values, "GET", "/api/v1/notifications/unread-count", "查询未读通知数量", "未読通知数を取得", "Get unread notification count");
        op(values, "PUT", "/api/v1/notifications/{id}/read", "标记通知为已读", "通知を既読にする", "Mark a notification as read");
        op(values, "PUT", "/api/v1/notifications/read-all", "标记全部通知为已读", "すべての通知を既読にする", "Mark all notifications as read");

        crud(values, "/api/v1/content/articles", "文章", "記事", "article", true);
        op(values, "GET", "/api/v1/content/categories", "查询全部分类", "すべてのカテゴリを取得", "List all categories");
        op(values, "POST", "/api/v1/content/categories", "创建分类", "カテゴリを作成", "Create a category");
        crudItem(values, "/api/v1/content/categories", "分类", "カテゴリ", "category");

        op(values, "GET", "/api/v1/analytics/overview", "查询数据分析概览", "分析概要を取得", "Get analytics overview");
        op(values, "GET", "/api/v1/data/overview", "查询数据中心概览", "データセンター概要を取得", "Get data-center overview");
        op(values, "GET", "/api/v1/audit/operations", "查询操作审计日志", "操作監査ログを取得", "List operation audit logs");

        op(values, "POST", "/api/v1/files", "上传文件", "ファイルをアップロード", "Upload a file");
        op(values, "GET", "/api/v1/files/{tenantId}/{filename}", "读取文件", "ファイルを取得", "Download a file");
        op(values, "DELETE", "/api/v1/files/{tenantId}/{filename}", "删除文件", "ファイルを削除", "Delete a file");

        op(values, "GET", "/api/v1/public/blog/{articleId}/comments", "查询博客评论", "ブログコメントを取得", "List blog comments");
        op(values, "POST", "/api/v1/blog/{articleId}/comments", "发表评论", "コメントを投稿", "Post a comment");
        op(values, "POST", "/api/v1/payments/checkout", "创建演示支付订单", "デモ決済注文を作成", "Create a demo checkout order");

        op(values, "GET", "/api/v1/public/blog", "查询门户博客列表", "ポータルブログ一覧を取得", "List portal blog posts");
        op(values, "GET", "/api/v1/public/blog/{id}", "查询门户博客详情", "ポータルブログ詳細を取得", "Get a portal blog post");
        op(values, "POST", "/api/v1/public/contact", "提交联系表单", "お問い合わせを送信", "Submit the contact form");
        op(values, "GET", "/api/v1/public/faq", "查询常见问答", "よくある質問を取得", "List frequently asked questions");
        op(values, "GET", "/api/v1/public/pricing", "查询套餐价格", "料金プランを取得", "List pricing plans");
        op(values, "GET", "/api/v1/public/features", "查询产品功能", "製品機能を取得", "List product features");
        op(values, "GET", "/api/v1/public/about", "查询关于我们信息", "会社情報を取得", "Get about information");
        op(values, "GET", "/api/v1/public/product", "查询产品信息", "製品情報を取得", "Get product information");
        op(values, "GET", "/api/v1/public/docs", "查询文档目录", "ドキュメント目次を取得", "Get documentation catalog");
        op(values, "GET", "/api/v1/public/home", "查询门户首页信息", "ポータルホーム情報を取得", "Get portal home content");

        op(values, "GET", "/api/v1/dashboard/stats", "查询仪表盘统计", "ダッシュボード統計を取得", "Get dashboard statistics");
        op(values, "GET", "/api/v1/log/login", "查询登录日志", "ログインログを取得", "List login logs");
        op(values, "GET", "/api/v1/log/operation", "查询操作日志", "操作ログを取得", "List operation logs");
        op(values, "GET", "/api/v1/log/error", "查询前端错误日志", "フロントエンドエラーログを取得", "List frontend error logs");
        op(values, "GET", "/api/v1/config/list", "查询系统参数", "システム設定を取得", "List system settings");
        op(values, "GET", "/api/v1/dept/list", "查询演示部门树", "デモ部門ツリーを取得", "Get demo department tree");
        op(values, "GET", "/api/v1/dict/list", "查询字典列表", "辞書一覧を取得", "List dictionaries");
        op(values, "GET", "/api/v1/notice/list", "查询通知公告", "お知らせ一覧を取得", "List announcements");
        op(values, "GET", "/api/v1/monitor/online", "查询在线用户", "オンラインユーザーを取得", "List online users");
        op(values, "GET", "/api/v1/monitor/server", "查询服务运行信息", "サーバー稼働情報を取得", "Get server runtime information");
        return Collections.unmodifiableMap(values);
    }

    private static void crud(Map<String, Text> values, String base, String zh, String ja, String en, boolean paged) {
        op(values, "GET", base, paged ? "分页查询" + zh : "查询" + zh + "列表",
                paged ? ja + "をページ検索" : ja + "一覧を取得",
                paged ? "Search " + en + "s with pagination" : "List " + en + "s");
        op(values, "POST", base, "创建" + zh, ja + "を作成", "Create a " + en);
        crudItem(values, base, zh, ja, en);
    }

    private static void crudItem(Map<String, Text> values, String base, String zh, String ja, String en) {
        op(values, "GET", base + "/{id}", "查询" + zh + "详情", ja + "詳細を取得", "Get " + en + " details");
        op(values, "PUT", base + "/{id}", "更新" + zh, ja + "を更新", "Update a " + en);
        op(values, "DELETE", base + "/{id}", "删除" + zh, ja + "を削除", "Delete a " + en);
    }

    private static void listCreateUpdateDelete(Map<String, Text> values, String base, String zh, String ja, String en) {
        op(values, "GET", base, "查询全部" + zh, "すべての" + ja + "を取得", "List all " + en + "s");
        op(values, "POST", base, "创建" + zh, ja + "を作成", "Create a " + en);
        op(values, "PUT", base + "/{id}", "更新" + zh, ja + "を更新", "Update a " + en);
        op(values, "DELETE", base + "/{id}", "删除" + zh, ja + "を削除", "Delete a " + en);
    }

    private static void op(Map<String, Text> values, String method, String path, String zh, String ja, String en) {
        String key = method + " " + path;
        if (values.put(key, t(zh, ja, en)) != null) {
            throw new IllegalStateException("Duplicate OpenAPI operation documentation: " + key);
        }
    }

    private static Map<String, Text> fields() {
        Map<String, Text> values = new LinkedHashMap<>();
        field(values, "id", "资源 ID", "リソース ID", "Resource ID");
        field(values, "tenantId", "租户 ID", "テナント ID", "Tenant ID");
        field(values, "tenantCode", "租户编码；不传时使用 default", "テナントコード。省略時は default", "Tenant code; defaults to `default`");
        field(values, "username", "用户名", "ユーザー名", "Username");
        field(values, "password", "密码", "パスワード", "Password");
        field(values, "nickname", "用户昵称", "表示名", "Display name");
        field(values, "email", "电子邮箱", "メールアドレス", "Email address");
        field(values, "phone", "手机号码", "電話番号", "Phone number");
        field(values, "status", "状态", "状態", "Status");
        field(values, "name", "名称", "名前", "Name");
        field(values, "code", "唯一编码", "一意のコード", "Unique code");
        field(values, "description", "详细说明", "詳細説明", "Detailed description");
        field(values, "title", "标题", "タイトル", "Title");
        field(values, "content", "内容", "内容", "Content");
        field(values, "summary", "摘要", "概要", "Summary");
        field(values, "keyword", "名称或内容关键字", "名前または内容のキーワード", "Name or content keyword");
        field(values, "page", "页码，从 1 开始", "ページ番号（1 から）", "Page number, starting at 1");
        field(values, "pageSize", "每页数量，最大 100", "1 ページの件数（最大 100）", "Items per page, maximum 100");
        field(values, "total", "符合条件的总数量", "該当する総件数", "Total matching items");
        field(values, "items", "当前页数据", "現在ページのデータ", "Current page items");
        field(values, "parentId", "父节点 ID；根节点为 0", "親ノード ID。ルートは 0", "Parent node ID; root is 0");
        field(values, "userId", "用户 ID", "ユーザー ID", "User ID");
        field(values, "projectId", "项目 ID", "プロジェクト ID", "Project ID");
        field(values, "articleId", "文章 ID", "記事 ID", "Article ID");
        field(values, "categoryId", "分类 ID", "カテゴリ ID", "Category ID");
        field(values, "departmentId", "部门 ID", "部門 ID", "Department ID");
        field(values, "filename", "存储文件名", "保存ファイル名", "Stored filename");
        field(values, "file", "待上传文件", "アップロードするファイル", "File to upload");
        field(values, "days", "统计最近天数", "集計対象の日数", "Number of recent days to analyze");
        field(values, "version", "乐观锁版本号", "楽観ロックのバージョン", "Optimistic-lock version");
        field(values, "Idempotency-Key", "幂等键；同一业务请求必须保持唯一", "冪等キー。同一業務リクエストでは一意にしてください", "Idempotency key; must uniquely identify the business request");
        field(values, "refreshToken", "用于换取新令牌或登出的 Refresh Token", "トークン更新またはログアウトに使用する Refresh Token", "Refresh Token used to renew tokens or log out");
        field(values, "accessToken", "访问受保护接口的 JWT", "保護 API にアクセスする JWT", "JWT used to access protected operations");
        field(values, "expiresIn", "Access Token 有效期，单位为秒", "Access Token の有効期間（秒）", "Access Token lifetime in seconds");
        field(values, "roles", "角色编码列表", "ロールコード一覧", "Role code list");
        field(values, "permissions", "权限编码列表", "権限コード一覧", "Permission code list");
        field(values, "roleIds", "要绑定的角色 ID 列表", "割り当てるロール ID 一覧", "Role IDs to assign");
        field(values, "permissionIds", "要绑定的权限 ID 列表", "割り当てる権限 ID 一覧", "Permission IDs to assign");
        field(values, "menuIds", "要绑定的菜单 ID 列表", "割り当てるメニュー ID 一覧", "Menu IDs to assign");
        field(values, "createdAt", "创建时间（ISO 8601）", "作成日時（ISO 8601）", "Creation time (ISO 8601)");
        field(values, "updatedAt", "最后更新时间（ISO 8601）", "最終更新日時（ISO 8601）", "Last update time (ISO 8601)");
        field(values, "startDate", "开始日期", "開始日", "Start date");
        field(values, "endDate", "结束日期", "終了日", "End date");
        field(values, "dueDate", "截止日期", "期限日", "Due date");
        field(values, "priority", "优先级：LOW、MEDIUM、HIGH 或 URGENT", "優先度：LOW、MEDIUM、HIGH、URGENT", "Priority: LOW, MEDIUM, HIGH, or URGENT");
        field(values, "comment", "审批意见", "承認コメント", "Approval comment");
        field(values, "channel", "支付渠道：alipay、wechat 或 card", "決済方法：alipay、wechat、card", "Payment channel: alipay, wechat, or card");
        field(values, "planId", "套餐 ID", "料金プラン ID", "Plan ID");
        field(values, "plan", "租户套餐编码", "テナントプランコード", "Tenant plan code");
        field(values, "expiredAt", "租户到期时间", "テナント有効期限", "Tenant expiration time");
        field(values, "path", "路由或请求路径", "ルートまたはリクエストパス", "Route or request path");
        field(values, "type", "业务类型", "業務タイプ", "Business type");
        field(values, "sort", "排序值，数值越小越靠前", "並び順。小さい値ほど先頭", "Sort order; lower values appear first");
        field(values, "visible", "是否在菜单中显示", "メニューに表示するか", "Whether the menu is visible");
        field(values, "read", "是否已读", "既読かどうか", "Whether the notification has been read");
        return Collections.unmodifiableMap(values);
    }

    private static void field(Map<String, Text> values, String name, String zh, String ja, String en) {
        values.put(name, t(zh, ja, en));
    }

    private static Object example(String name) {
        return switch (name) {
            case "id", "userId", "projectId", "articleId", "categoryId", "departmentId", "tenantId", "planId" -> 1;
            case "page" -> 1;
            case "pageSize" -> 20;
            case "days" -> 7;
            case "version" -> 1;
            case "tenantCode" -> "default";
            case "username" -> "admin";
            case "email" -> "admin@example.com";
            case "status" -> "active";
            case "Idempotency-Key" -> "550e8400-e29b-41d4-a716-446655440000";
            default -> null;
        };
    }
}
