# fast-vue3-server

[English](./README.md) · [简体中文](./README.zh-CN.md) · [繁體中文（台灣）](./README.zh-TW.md) · [繁體中文（香港）](./README.zh-HK.md) · [日本語](./README.ja.md)

Official Java backend for Fast Vue3, using a shared `/api/v1` contract.

Java 21, Spring Boot 3.5.16, Spring MVC/Security, MyBatis-Plus, PostgreSQL 16, Redis 7, Flyway, JUnit 5 and Testcontainers. Modules stay in `module/<domain>/{api,service,persistence}`; controllers expose DTOs and services own transactions.

[Documentation](https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/) · [Configuration](https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/guide/configuration) · [API contracts](https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/api/contracts)

## Development

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

## Build and test

```bash
./mvnw spotless:apply
./mvnw spotless:check checkstyle:check
./mvnw -DskipTests test-compile pmd:check spotbugs:check
./mvnw test
./mvnw clean verify
```

Tool responsibilities: Spotless 3.10.4 uses google-java-format 1.28.0 with AOSP four-space formatting and import cleanup; Checkstyle retains the project’s basic conventions; Maven PMD 3.28.0 (PMD 7.17.0) analyzes production and test resources, unused code and cognitive complexity (method threshold 25); SpotBugs 4.10.5.0 analyzes production bytecode at its default confidence threshold; Error Prone 2.43.0 enforces the 21 configured error checks during Java 21 compilation alongside Lombok/MapStruct.

`validate` checks formatting and Checkstyle; production/test compilation runs Error Prone; `verify` runs PMD/SpotBugs. Builds never rewrite sources: run `spotless:apply` explicitly. Static checks do not require Docker and exclude generated code. Rules live in `config/`; CI archives `target/checkstyle-result.xml`, `target/pmd.xml`, and `target/spotbugsXml.xml`.

Exclusions are limited to named types, methods and patterns: existing passive request/response collection ownership is preserved for the listed DTOs; shared Spring constructor dependencies, validated request-ID headers and conditional CORS defensive copies have individual explanations. PMD recognizes executor shutdown in `finally`; only the Mockito JDBC fixture suppresses resource-close warnings locally. Null, resource and other bug detection remain active elsewhere.

`test-compile` checks production and test types without executing tests. Compilation fails on unchecked conversions, raw types, deprecated API warnings and unmapped MapStruct target fields. `test` runs JUnit/Mockito/MockMvc without Docker. `clean verify` additionally runs PostgreSQL/Redis integration tests through Failsafe and packages the executable jar; Docker is required and missing infrastructure fails verification instead of silently skipping tests. JaCoCo writes combined coverage to `target/site/jacoco/index.html` and `jacoco.xml`. Local checks do not establish remote CI results.

## Production

Production uses the independent `compose.prod.yml`. Do not merge it with the development Compose file. Fill every required secret in a private `.env.prod` or inject it from a secret manager. Use a random JWT secret of at least 32 bytes and a bootstrap password of at least 12 characters, at most 72 UTF-8 bytes. CORS accepts explicit HTTPS origins only. Startup rejects development profiles, placeholder secrets and demo adapters.

```bash
cp .env.prod.example .env.prod
# Supply required secrets and explicit HTTPS origins before running.
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

Health and liveness/readiness are public without infrastructure details. Readiness checks PostgreSQL and Redis; liveness checks application state only. Metrics and Prometheus require `audit:view`. Production logs use ECS JSON with request ID, tenant/user context and HTTP latency. Sensitive headers and request bodies are not logged. Trace export is not configured; there is no claim of full OpenTelemetry tracing.

Production disables demo dashboard/analytics/portal fixtures and returns 503 for demonstration payment checkout without writing an order. A real payment provider, signed callbacks, ledger and payment idempotency require business integration; this checkout cannot accept real money.

## Runtime budgets and notification recovery

- Lock waits default to 5 seconds, SQL to 15 seconds and transactions to 20 seconds. Configure `DB_LOCK_TIMEOUT_MILLIS`, `DB_STATEMENT_TIMEOUT_SECONDS`, `DB_TRANSACTION_TIMEOUT_SECONDS`; startup validates that order. A transaction timeout bounds database work, not the entire HTTP request.
- Storage and Servlet share `FILE_MAX_SIZE_BYTES` (10 MiB). `FILE_MAX_REQUEST_SIZE_BYTES` defaults to 11 MiB and must allow at least 64 KiB of multipart overhead. Tomcat uses the same finite request budget to drain rejected uploads so clients can read 413 within that budget. Missing `file` returns 400; oversized uploads return 413.
- ID collections accept up to 1000 non-null IDs; duplicate role grants are normalized. Articles accept up to 1000 paragraphs of 10000 characters and 100 tags of 64 characters. Omitted update collections retain their values; empty arrays clear them.
- Notifications enter a PostgreSQL outbox in the business transaction, with an immediate delivery attempt after commit. Background polling defaults to 20 rows every 5 seconds. Notification insertion and acknowledgement commit atomically; row locks coordinate instances. Failures back off exponentially from 5 seconds to at most 1 hour and become `FAILED` after 10 attempts. Configure `NOTIFICATION_BATCH_SIZE`, `NOTIFICATION_MAX_ATTEMPTS`, `NOTIFICATION_POLL_INTERVAL_MS`.
- Inspect `sys_notification_outbox.status/attempts/last_error` (exception class only). After resolving the cause, requeue a specific failed UUID by setting `status='PENDING', attempts=0, next_attempt_at=now(), last_error=NULL` with `WHERE id=? AND status='FAILED'`. Never replay delivered rows. Retain/purge delivered history according to operational policy; preserve unfinished rows.
- V12 adds live category-name uniqueness per tenant. Before deployment, run `SELECT tenant_id,name,count(*) FROM content_category WHERE NOT deleted GROUP BY tenant_id,name HAVING count(*) > 1;` and resolve duplicates with the business owner. Migration fails rather than renaming or deleting data. V13 adds the notification outbox.
- `clean verify` requires at least 90% line / 70% branch coverage per article, category, menu, rate-limit and notification-dispatch service class; the department service requires 85% / 70%. Other modules remain reported while coverage gates expand incrementally.

[Architecture and enterprise API](https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/api/enterprise): thin Controller → Service → MyBatis Mapper; DTO/persistence boundaries, tenant isolation, resource ownership, service transactions, optimistic locking and durable command idempotency. Schema changes are append-only Flyway migrations. Shared API changes must include frontend clients, Nitro mock and tests. Do not commit credentials or generated files.
