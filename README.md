# fast-vue3-server

**Languages:** English | [简体中文](./README.zh-CN.md) | [繁體中文（台灣）](./README.zh-TW.md) | [繁體中文（香港）](./README.zh-HK.md) | [日本語](./README.ja.md)

The official Spring Boot reference backend for [fast-vue3](https://github.com/tobe-fe-dalao/fast-vue3). It is a Java 21 modular monolith that provides one `/api/v1` contract for all admin and site applications.

Documentation: <https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/>

## Stack

- Spring Boot 3 / Spring MVC / Spring Security
- JWT access and refresh tokens
- MyBatis-Plus and PostgreSQL
- Redis refresh-token storage
- Flyway migrations
- Springdoc OpenAPI
- JUnit 5, MockMvc, and Testcontainers

## Modules

```text
com.fastvue
├── common          # Response envelope, errors, exception handling
├── config          # Persistence, OpenAPI, initialization
├── infrastructure  # Audit fields and entity base
├── security        # JWT and Spring Security
└── module
    ├── auth, user, role, permission, menu
    ├── content, analytics, portal
    └── site        # Blog comments and payment checkout
```

## Run locally

For local Java development, install JDK 21 and Docker.

```bash
cp .env.example .env # first run only
docker compose up -d
./mvnw spring-boot:run
```

Without a local JDK, run everything with Docker Desktop or OrbStack. Both expose the same Docker Compose CLI:

```bash
docker compose --profile app up -d --build
docker compose ps
docker compose logs -f app
```

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Development login: `admin / admin123`

## Connect fast-vue3

```bash
cd /Users/fong/Workspace/personal/frontend/vue/fast-vue3/fast-vue3
VITE_DEV_BACKEND=server pnpm dev:web-antd
# or
VITE_DEV_BACKEND=server pnpm dev:site-antd
```

The frontend keeps `/api/v1` as its base path; shared Vite configuration proxies it to this server.

## Response contract

```json
{ "code": 0, "message": "success", "data": {} }
```

Pagination uses:

```json
{ "items": [], "page": 1, "pageSize": 20, "total": 0 }
```

## Authentication boundary

Public authentication endpoints:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`

Public site content:

- `GET /api/v1/public/home|product|features|about|docs|faq|pricing`
- `GET /api/v1/public/blog`
- `GET /api/v1/public/blog/{id}`
- `GET /api/v1/public/blog/{id}/comments`
- `POST /api/v1/public/contact`

Authenticated site interactions:

- `POST /api/v1/blog/{id}/comments`
- `POST /api/v1/payments/checkout`

All management and content-management endpoints require an access token, followed by method-level RBAC checks where applicable.

Operating-data endpoints additionally require `analytics:view` or `data:view`. `GET /api/v1/menus` intentionally needs only login because it returns the current user's own navigation; the management tree and menu CRUD retain `menu:*` permissions. Public handlers do not call the throwing `SecurityUtils.currentUser()` API.

## Payment design

Checkout accepts a payable `planId` and `channel` (`alipay`, `wechat`, or `card`), persists a pending order, and returns an order number, amount, expiry time, and `checkoutUrl`. The bundled adapter never charges real money. A production integration should replace the demo URL with a provider session and verify signed, idempotent callbacks before marking an order paid.

## Persistence

Flyway owns the schema. `V7__site_interactions.sql` adds `site_blog_comment` and `site_payment_order`. Both tables record the authenticated user and common audit fields.

## Tests

```bash
./mvnw test
```

- Unit tests cover authentication and RBAC services.
- MockMvc tests verify response contracts and anonymous/authenticated boundaries.
- Testcontainers runs CRUD and migration checks against PostgreSQL when Docker is available.

## Documentation

Architecture, setup, configuration, testing, and API reference documentation is maintained centrally in [fast-vue3-site](https://tobe-fe-dalao.github.io/fast-vue3-site/en/server/). This repository intentionally keeps only concise operational guidance in its README and does not include a Node.js/VitePress toolchain.

## License

[MIT](./LICENSE)
