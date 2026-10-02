# fast-vue3-server

[English](./README.md) · [简体中文](./README.zh-CN.md) · [繁體中文（台灣）](./README.zh-TW.md) · [繁體中文（香港）](./README.zh-HK.md) · [日本語](./README.ja.md)

Fast Vue3 的 Java 後端，提供共用 `/api/v1` 契約。

技術基線為 Java 21、Spring Boot 3.5.16、Spring MVC/Security、MyBatis-Plus、PostgreSQL 16、Redis 7、Flyway、JUnit 5 與 Testcontainers。保留 `module/<領域>/{api,service,persistence}`，Controller 回傳 DTO，Service 管理業務與交易。

[Documentation](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/) · [Configuration](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/guide/configuration) · [API contracts](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/api/contracts)

## 本機開發

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

## 建置與測試

```bash
./mvnw spotless:apply
./mvnw spotless:check checkstyle:check
./mvnw -DskipTests test-compile pmd:check spotbugs:check
./mvnw test
./mvnw clean verify
```

工具分工：Spotless 3.10.4 使用 google-java-format 1.28.0 的 AOSP 4 空格風格整理格式與匯入；Checkstyle 保留專案基本規範；PMD 外掛 3.28.0（PMD 7.17.0）檢查主程式與測試的資源管理、未使用程式碼及認知複雜度（方法門檻 25）；SpotBugs 4.10.5.0 以預設信賴門檻分析正式程式的位元碼；Error Prone 2.43.0 在 Java 21 編譯時執行設定中的 21 項錯誤規則，與既有 Lombok/MapStruct 處理器共存。

`validate` 執行格式與 Checkstyle 檢查，主程式及測試編譯時執行 Error Prone，`verify` 執行 PMD/SpotBugs。建置只檢查，格式修正須明確執行 `spotless:apply`。上述靜態檢查無需 Docker，產生的程式碼不納入分析。規則位於 `config/`；CI 歸檔 `target/checkstyle-result.xml`、`target/pmd.xml`、`target/spotbugsXml.xml`。

排除規則限定類型、方法與告警：保留既有請求／回應 DTO 的集合參照約定，僅排除明列類型的集合暴露告警；共用 Spring Bean 建構子的參照、已驗證請求 ID 的回應標頭、CORS 條件防禦性複製均有個別說明。PMD 認可 `finally` 中的執行緒池關閉；純 Mockito JDBC 測試替身僅局部排除資源關閉告警。其他空值、資源與缺陷檢測持續啟用。

`test-compile` 僅檢查主程式與測試程式的編譯型別；未檢查轉換、原始泛型、過時 API 警告及 MapStruct 目標欄位遺漏會使編譯失敗。`test` 執行 JUnit/Mockito/MockMvc，無需 Docker。`clean verify` 另透過 Failsafe 執行 PostgreSQL/Redis 整合測試並打包；必須具備 Docker，環境缺少時會失敗，不再靜默略過。JaCoCo 將合併覆蓋率寫入 `target/site/jacoco/index.html` 與 `jacoco.xml`。本機驗證不代表遠端 CI 結果。

## Production

正式環境使用獨立 `compose.prod.yml`，不可與開發 Compose 合併。透過私有 `.env.prod` 或密鑰管理提供必要設定。JWT 隨機密鑰至少 32 位元組；初始化密碼至少 12 個字元，最多 72 個 UTF-8 位元組。CORS 僅接受明確的 HTTPS Origin。啟動拒絕開發 Profile、占位密鑰及示範適配器。

```bash
cp .env.prod.example .env.prod
# Supply required secrets and explicit HTTPS origins before running.
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

Health 與 liveness/readiness 可匿名讀取，不顯示基礎設施細節。Readiness 檢查 PostgreSQL、Redis；liveness 僅檢查應用狀態。Metrics 與 Prometheus 需要 `audit:view`。正式 ECS JSON 日誌包含請求 ID、租戶/使用者與 HTTP 延遲，不記錄敏感標頭或請求本文。未設定 Trace 匯出，並非完整 OpenTelemetry 追蹤。

正式環境關閉示範儀表板、分析與入口網站靜態資料；示範付款回傳 503，且不寫訂單。真實金流、簽章回呼、帳務明細與付款冪等需要業務整合；現有流程不能收取真實款項。

## 執行限制與通知復原

- 鎖定等待預設 5 秒、SQL 15 秒、交易 20 秒，分別由 `DB_LOCK_TIMEOUT_MILLIS`、`DB_STATEMENT_TIMEOUT_SECONDS`、`DB_TRANSACTION_TIMEOUT_SECONDS` 設定；啟動時會檢查順序。交易逾時不等於整個 HTTP 請求的截止時間。
- 業務層與 Servlet 共用 `FILE_MAX_SIZE_BYTES`（10 MiB）；`FILE_MAX_REQUEST_SIZE_BYTES` 預設 11 MiB，至少多留 64 KiB 給 multipart。Tomcat 使用相同的有限請求預算讀完遭拒的上傳內容，讓預算內的超限上傳可完整回傳 413。缺少 `file` 回傳 400，超過限制回傳 413。
- ID 集合最多 1000 個，禁止 null 元素，重複角色授權會去重。文章最多 1000 段、每段 10000 字元，標籤最多 100 個、每個 64 字元。更新時省略集合保留原值，空陣列清空。
- 通知與業務交易一起寫入 PostgreSQL outbox，提交後立即嘗試投遞；背景預設每 5 秒處理 20 筆。通知與投遞確認在同一交易提交，透過資料列鎖協調多個執行個體。失敗從 5 秒指數退避，最長間隔 1 小時，預設 10 次後保留為 `FAILED`。可調整 `NOTIFICATION_BATCH_SIZE`、`NOTIFICATION_MAX_ATTEMPTS`、`NOTIFICATION_POLL_INTERVAL_MS`。
- 查閱 `sys_notification_outbox` 的 `status/attempts/last_error`；錯誤僅記錄例外類別。排除原因後，僅針對指定 UUID 的 `FAILED` 資料列設定 `status='PENDING', attempts=0, next_attempt_at=now(), last_error=NULL`。不要重播已投遞資料；依保存政策清理已完成紀錄，保留待處理紀錄。
- V12 新增租戶內有效分類名稱唯一索引。部署前執行 `SELECT tenant_id,name,count(*) FROM content_category WHERE NOT deleted GROUP BY tenant_id,name HAVING count(*) > 1;`，由業務方處理重複名稱；移轉遇到重複會失敗，不會自動改名或刪除。V13 新增通知 outbox。
- `clean verify` 對文章、分類、選單、限流及通知投遞服務逐類要求行覆蓋率 90%、分支 70%；部門服務為 85%/70%。其餘模組仍產生覆蓋率報告。

[Architecture and enterprise API](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/api/enterprise): thin Controller → Service → MyBatis Mapper; DTO/persistence boundaries, tenant isolation, resource ownership, service transactions, optimistic locking and durable command idempotency. Schema changes are append-only Flyway migrations. Shared API changes must include frontend clients, Nitro mock and tests. Do not commit credentials or generated files.
