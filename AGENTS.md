# Agent Notes

This repository is the Java 21 Spring Boot reference backend for Fast Vue3. It exposes the shared `/api/v1` contract used by the `fast-vue3` frontend applications and Nitro mock.

## Repository Shape

- `src/main/java/com/fastvue/common/response`: response envelopes and pagination models.
- `src/main/java/com/fastvue/common/exception`: errors and global exception handling.
- `src/main/java/com/fastvue/config`: persistence, OpenAPI, serialization, and application configuration.
- `src/main/java/com/fastvue/infrastructure/persistence`: shared persistence infrastructure and audit fields.
- `src/main/java/com/fastvue/security`: JWT authentication, Spring Security, current-user access, and refresh-token storage.
- `src/main/java/com/fastvue/module`: business modules organized vertically by capability. Each module uses `api`, `service`, and `persistence` subpackages as needed.
- `src/main/resources/db/migration`: append-only Flyway migrations.
- `src/test`: unit, MockMvc, security, and integration tests.

## Common Commands

- Start PostgreSQL and Redis with `docker compose up -d`.
- Run the API locally with `./mvnw spring-boot:run` (JDK 21 required).
- Run the complete container profile with `docker compose --profile app up -d --build`.
- Run tests with `./mvnw test`.
- Run the release-quality Maven lifecycle with `./mvnw clean verify`.
- Check health at `http://localhost:8080/actuator/health` and Swagger at `http://localhost:8080/swagger-ui.html`.

## Architecture And API Conventions

- Keep code grouped by business module. Controllers handle HTTP concerns, services own business logic, and mappers own data access; controllers must not call mappers directly.
- Prefer Java 21 records for request and response DTOs where practical, and validate input with Jakarta Validation.
- Do not expose persistence entities from controllers. Convert them to response models or VOs.
- Business responses use `{ code, message, data }`; paginated data uses `{ items, page, pageSize, total }`.
- Public endpoints are limited to authentication bootstrap, health/OpenAPI, and `/api/v1/public/**`. All other routes require an access token by default, with method-level permissions for protected operations.
- Refresh tokens are not access tokens. Preserve the distinction in filters, services, tests, and documentation.
- Public handlers must use nullable current-user access when audit data is optional; do not call an accessor that throws for anonymous requests.

## Database And Configuration

- Flyway owns the schema. Add a new ordered migration for schema or seed changes; never rewrite a migration that may already have run.
- Keep secrets and environment-specific values in environment variables. Never commit `.env`, production credentials, JWT secrets, or real payment credentials.
- The bundled checkout flow is a demonstration adapter. Production payment work requires provider-side session creation, signed callback verification, idempotency, and explicit state transitions.

## Cross-Repository Contract

- `fast-vue3/packages/effects/api` is the frontend contract source. Contract changes must update Java DTOs/VOs, Nitro mock responses, frontend types and clients, and tests together.
- Long-form documentation is owned by the sibling `fast-vue3-site` repository under `docs/<locale>/server/` and is published at `https://tobe-fe-dalao.github.io/fast-vue3-site/`.
- Do not add a local `docs/` tree, VitePress dependency, Node package manifest, or documentation deployment workflow to this repository. Keep README files concise and link to the central site.

## Generated And Local Files

Do not commit `target/`, IDE metadata, `.env`, logs, or other generated/local state. Maven Wrapper files, including `.mvn/wrapper/maven-wrapper.jar`, are source-controlled project infrastructure.
