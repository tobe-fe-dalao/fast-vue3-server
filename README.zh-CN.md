# fast-vue3-server

[English](./README.md) · [简体中文](./README.zh-CN.md) · [繁體中文（台灣）](./README.zh-TW.md) · [繁體中文（香港）](./README.zh-HK.md) · [日本語](./README.ja.md)

Fast Vue3 的 Java 服务端，提供共享 `/api/v1` 契约。

技术基线为 Java 21、Spring Boot 3.5.16、Spring MVC/Security、MyBatis-Plus、PostgreSQL 16、Redis 7、Flyway、JUnit 5 与 Testcontainers。保留 `module/<业务域>/{api,service,persistence}` 模块，Controller 返回 DTO，Service 管理业务与事务。

[Documentation](https://tobe-fe-dalao.github.io/fast-vue3-site/zh/server/) · [Configuration](https://tobe-fe-dalao.github.io/fast-vue3-site/zh/server/guide/configuration) · [API contracts](https://tobe-fe-dalao.github.io/fast-vue3-site/zh/server/api/contracts)

## 本地开发

```bash
cp .env.example .env
docker compose --profile app up -d --build
```

Java 21:

```bash
docker compose up -d
set -a
source .env
set +a
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html` (development)
- Development only: `admin / admin123`

## 构建与测试

```bash
./mvnw spotless:apply
./mvnw spotless:check checkstyle:check
./mvnw -DskipTests test-compile pmd:check spotbugs:check
./mvnw test
./mvnw clean verify
```

质量工具分工：Spotless 3.10.4 使用 google-java-format 1.28.0 的 AOSP 4 空格风格，自动整理格式与导入；Checkstyle 保留项目基础规范；PMD 插件 3.28.0（PMD 7.17.0）检查主代码和测试的资源管理、无用代码及认知复杂度（方法阈值 25）；SpotBugs 4.10.5.0 按默认置信度分析生产字节码；Error Prone 2.43.0 在 Java 21 编译时执行配置中的 21 项错误规则，与现有 Lombok/MapStruct 处理器共存。

`validate` 执行格式和 Checkstyle 检查，主代码与测试编译时运行 Error Prone，`verify` 执行 PMD/SpotBugs。构建只检查，格式修复需显式执行 `spotless:apply`。以上静态检查无需 Docker，生成代码排除在分析范围外。规则位于 `config/`；报告为 `target/checkstyle-result.xml`、`target/pmd.xml`、`target/spotbugsXml.xml`，CI 统一归档。

排除规则按类型、方法和规则限定：保留现有请求/响应 DTO 的集合引用约定，仅排除列明类型的集合暴露告警；共享 Spring Bean 的构造器引用、已校验请求 ID 的响应头写入、CORS 条件防御性复制均有单独说明。PMD 认可 `finally` 中的线程池关闭，纯 Mockito JDBC 夹具仅局部排除资源关闭告警。未全局关闭空值、资源或其他缺陷检测。

`test-compile` 只检查主代码和测试代码的编译类型；未检查转换、原始泛型、过时 API 告警及 MapStruct 目标字段遗漏会导致编译失败。`test` 运行 JUnit/Mockito/MockMvc，无需 Docker。`clean verify` 另外通过 Failsafe 执行 PostgreSQL/Redis 集成测试并打包；必须有 Docker，环境缺失会失败，不再静默跳过。JaCoCo 将合并覆盖率写入 `target/site/jacoco/index.html` 和 `jacoco.xml`。本地验证不代表远端 CI 结果。

## Production

生产使用独立的 `compose.prod.yml`，不要与开发 Compose 合并。通过私有 `.env.prod` 或密钥管理服务提供所有必填配置。JWT 随机密钥至少 32 字节；初始化密码至少 12 个字符，最多 72 个 UTF-8 字节。CORS 只接受明确的 HTTPS Origin。启动时拒绝开发 Profile、占位密钥和演示适配器。

```bash
cp .env.prod.example .env.prod
# Supply required secrets and explicit HTTPS origins before running.
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

Health 和 liveness/readiness 匿名开放，不返回基础设施细节。Readiness 检查 PostgreSQL、Redis，liveness 只检查应用状态。Metrics 与 Prometheus 需要 `audit:view`。生产 ECS JSON 日志包含请求 ID、租户/用户上下文和 HTTP 延迟，不记录敏感请求头或请求体。没有配置 Trace 导出，不能视为完整 OpenTelemetry 链路追踪。

生产关闭演示仪表盘、分析及门户静态数据；演示支付结算返回 503，且不写订单。真实支付渠道、签名回调、流水及支付幂等需要业务集成；现有结算不能收取真实资金。

## 运行边界与通知恢复

- 数据库默认锁等待 5 秒、SQL 15 秒、事务 20 秒；通过 `DB_LOCK_TIMEOUT_MILLIS`、`DB_STATEMENT_TIMEOUT_SECONDS`、`DB_TRANSACTION_TIMEOUT_SECONDS` 调整，启动时校验顺序。事务超时约束数据库操作，不是整个 HTTP 请求的强制截止时间。
- 上传业务层与 Servlet 共用 `FILE_MAX_SIZE_BYTES`（默认 10 MiB）；`FILE_MAX_REQUEST_SIZE_BYTES` 默认 11 MiB，至少额外保留 64 KiB 给 multipart 元数据。Tomcat 以同一有限请求预算吞掉被拒绝的上传体，使预算内的超限上传能完整返回 413。缺少 `file` 返回 400，超限返回 413。
- 权限、菜单、角色 ID 集合最多 1000 个，拒绝 null 元素；重复角色授权 ID 去重。文章正文最多 1000 段、每段 10000 字符，标签最多 100 个、每个 64 字符。更新集合缺省保持原值，空数组清空。
- 通知先与业务事务一起写入 PostgreSQL outbox，提交后立即尝试投递；后台默认每 5 秒处理最多 20 条。写通知与确认投递在同一事务提交；多实例通过行锁避免重复消费。失败按 5 秒指数退避，最多间隔 1 小时、默认尝试 10 次；耗尽后保留为 `FAILED`。`NOTIFICATION_BATCH_SIZE`、`NOTIFICATION_MAX_ATTEMPTS`、`NOTIFICATION_POLL_INTERVAL_MS` 可调。
- 排障时查询 `sys_notification_outbox` 的 `status/attempts/last_error`（仅异常类名）；修复原因后按指定 UUID 将 `FAILED` 更新为 `PENDING`，重置 `attempts=0, next_attempt_at=now(), last_error=NULL`。不要重放 `DELIVERED`。根据保留策略清理已投递历史行；禁止删除未完成行。
- V12 新增租户内有效分类名称唯一索引。部署前用 `SELECT tenant_id,name,count(*) FROM content_category WHERE NOT deleted GROUP BY tenant_id,name HAVING count(*) > 1;` 检查并由业务方处理历史重复名；迁移遇到重复会失败，不会自动改名或删数据。V13 新增通知 outbox。
- `clean verify` 对文章、分类、菜单、限流器、通知投递器逐类要求行覆盖率至少 90%、分支至少 70%；部门服务至少 85%/70%。其余模块保留覆盖率报告，逐步扩大门禁范围。

[Architecture and enterprise API](https://tobe-fe-dalao.github.io/fast-vue3-site/zh/server/api/enterprise): thin Controller → Service → MyBatis Mapper; DTO/persistence boundaries, tenant isolation, resource ownership, service transactions, optimistic locking and durable command idempotency. Schema changes are append-only Flyway migrations. Shared API changes must include frontend clients, Nitro mock and tests. Do not commit credentials or generated files.
